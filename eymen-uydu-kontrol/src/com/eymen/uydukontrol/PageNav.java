package com.eymen.uydukontrol;
public final class PageNav {
 private PageNav(){}
 public static int jump(int index,int count,int direction){
  if(count<=0)return 0;
  int last=(count-1)/10;
  int page=Math.max(0,Math.min(last,index/10+direction));
  return page*10;
 }
}
