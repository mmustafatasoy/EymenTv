#!/usr/bin/env python3
"""
EYMEN TV hazır internet kanal listesi.

Türkiye'nin herkese açık, ücretsiz canlı yayın adreslerini (iptv-org dizini) indirir,
her yayını tek tek dener ve yalnızca yanıt verenleri channels/tr.m3u dosyasına yazar.
GitHub Actions bunu her gün çalıştırır; uygulama listeyi bu dosyadan alır.

Kaynak: https://github.com/iptv-org/iptv (yalnızca yayıncıların herkese açtığı bağlantılar)
"""
import concurrent.futures
import datetime
import re
import ssl
import sys
import unicodedata
import urllib.error
import urllib.request
from pathlib import Path

SOURCES = [
    "https://iptv-org.github.io/iptv/countries/tr.m3u",
]
OUT = Path(__file__).resolve().parent / "tr.m3u"
REPORT = Path(__file__).resolve().parent / "report.md"
UA = "Mozilla/5.0 (Linux; Android 11; Android TV) EYMEN-TV/2.1"
EXCLUDE_GROUP_WORDS = ("xxx", "adult")
CATEGORY_ORDER = ["Haber", "Spor", "Çocuk", "Belgesel", "Müzik", "Film", "Dini", "Genel"]
GROUP_MAP = [
    ("news", "Haber"), ("sport", "Spor"), ("kids", "Çocuk"), ("animation", "Çocuk"),
    ("documentary", "Belgesel"), ("music", "Müzik"), ("movies", "Film"), ("series", "Film"),
    ("religious", "Dini"),
]
ATTR = re.compile(r'([A-Za-z0-9_-]+)="([^"]*)"')
CTX = ssl.create_default_context()


def clean_name(name: str) -> str:
    n = re.sub(r"\[[^\]]*\]", "", name)
    n = re.sub(r"\(\s*\d{3,4}p\s*\)", "", n)
    return re.sub(r"\s+", " ", n).strip()


def norm(name: str) -> str:
    n = unicodedata.normalize("NFD", name)
    n = "".join(ch for ch in n if unicodedata.category(ch) != "Mn").lower().replace("ı", "i")
    n = re.sub(r"\b(hd|sd|fhd|uhd|4k)\b", " ", n)
    return re.sub(r"[^a-z0-9]", "", n)


def turkish_group(raw: str) -> str:
    g = (raw or "").lower()
    for word, cat in GROUP_MAP:
        if word in g:
            return cat
    return "Genel"


def fetch_text(url: str) -> str:
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=30, context=CTX) as r:
        return r.read().decode("utf-8", "replace")


def parse(text: str):
    entries = []
    info = None
    needs_headers = False
    for raw in text.splitlines():
        line = raw.strip().lstrip("﻿")
        if not line:
            continue
        if line.upper().startswith("#EXTINF"):
            attrs = {k.lower(): v for k, v in ATTR.findall(line)}
            name = line.rsplit(",", 1)[1].strip() if "," in line else attrs.get("tvg-name", "")
            info = {"name": name, "attrs": attrs}
            needs_headers = False
            continue
        if line.startswith("#EXTVLCOPT") or line.startswith("#KODIPROP"):
            needs_headers = True
            continue
        if line.startswith("#"):
            continue
        if info and line.lower().startswith(("http://", "https://")):
            entries.append({**info, "url": line, "needs_headers": needs_headers})
        info = None
        needs_headers = False
    return entries


def check(url: str):
    """Returns (status, detail). status: ok | geo | dead"""
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "*/*"})
    try:
        with urllib.request.urlopen(req, timeout=10, context=CTX) as r:
            code = r.status
            ctype = (r.headers.get("Content-Type") or "").lower()
            body = r.read(4096)
            if 200 <= code < 300 and (b"#EXTM3U" in body or "mpegurl" in ctype or ctype.startswith("video/")
                                      or "octet-stream" in ctype or "mp2t" in ctype):
                return "ok", str(code)
            return "dead", f"HTTP {code} {ctype[:30]}"
    except urllib.error.HTTPError as e:
        if e.code in (401, 403, 451):
            return "geo", f"HTTP {e.code}"
        return "dead", f"HTTP {e.code}"
    except Exception as e:  # timeouts, DNS, TLS, connection reset
        return "dead", type(e).__name__


def main() -> int:
    entries = []
    for src in SOURCES:
        try:
            entries += parse(fetch_text(src))
        except Exception as e:
            print(f"Kaynak indirilemedi: {src}: {e}", file=sys.stderr)
    if not entries:
        print("Hiç kanal bulunamadı; mevcut liste korunuyor.", file=sys.stderr)
        return 1

    candidates, seen_urls = [], set()
    for e in entries:
        group = e["attrs"].get("group-title", "")
        if any(w in group.lower() for w in EXCLUDE_GROUP_WORDS):
            continue
        if e["needs_headers"] or e["url"] in seen_urls:
            continue
        seen_urls.add(e["url"])
        name = clean_name(e["name"])
        if not name:
            continue
        candidates.append({**e, "name": name, "key": norm(name), "group": turkish_group(group)})

    with concurrent.futures.ThreadPoolExecutor(max_workers=32) as pool:
        results = list(pool.map(lambda c: check(c["url"]), candidates))

    best = {}
    rank = {"ok": 0, "geo": 1, "dead": 2}
    dropped = []
    for c, (status, detail) in zip(candidates, results):
        c["status"], c["detail"] = status, detail
        if status == "dead":
            dropped.append(c)
            continue
        old = best.get(c["key"])
        if old is None or rank[status] < rank[old["status"]]:
            best[c["key"]] = c

    channels = sorted(
        best.values(),
        key=lambda c: (CATEGORY_ORDER.index(c["group"]) if c["group"] in CATEGORY_ORDER else 99, c["key"]),
    )
    ok = sum(1 for c in channels if c["status"] == "ok")
    geo = len(channels) - ok
    if ok < 5:
        print(f"Yalnızca {ok} kanal çalışıyor; bu bir ağ sorunu olabilir, mevcut liste korunuyor.", file=sys.stderr)
        return 1

    now = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    lines = [
        "#EXTM3U",
        f"# EYMEN TV hazır internet listesi · {now} · {ok} çalışan, {geo} yalnızca Türkiye'den açılabilir",
        "# Kaynak: iptv-org (yayıncıların herkese açtığı ücretsiz yayınlar). Her gün yeniden denenir.",
    ]
    for c in channels:
        a = c["attrs"]
        logo = a.get("tvg-logo", "")
        tvg_id = a.get("tvg-id", "")
        lines.append(
            f'#EXTINF:-1 tvg-id="{tvg_id}" tvg-logo="{logo}" group-title="{c["group"]}" eymen-durum="{c["status"]}",{c["name"]}'
        )
        lines.append(c["url"])
    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")

    report = [
        "# Kanal listesi raporu",
        "",
        f"- Tarih: {now}",
        f"- Denenen yayın: {len(candidates)}",
        f"- Listede: {len(channels)} kanal ({ok} çalışıyor, {geo} Türkiye dışından kapalı, TV'de yeniden denenir)",
        f"- Çalışmadığı için çıkarılan: {len(dropped)}",
        "",
        "## Listedeki kanallar",
        "",
    ]
    report += [f"- {c['group']} · {c['name']} · {'çalışıyor' if c['status'] == 'ok' else 'Türkiye dışından kapalı'}" for c in channels]
    report += ["", "## Çıkarılanlar", ""]
    report += [f"- {c['name']} · {c['detail']}" for c in sorted(dropped, key=lambda c: c["key"])]
    REPORT.write_text("\n".join(report) + "\n", encoding="utf-8")
    print(f"{len(channels)} kanal yazıldı ({ok} çalışıyor, {geo} geo), {len(dropped)} çıkarıldı.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
