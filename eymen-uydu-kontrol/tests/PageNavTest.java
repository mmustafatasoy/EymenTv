import com.eymen.uydukontrol.PageNav;
public class PageNavTest {
 public static void main(String[] args){
  for(int count:new int[]{0,1,9,10,11,19,20,21,589,590}){
   for(int i=0;i<Math.max(1,count);i++){
    int right=PageNav.jump(i,count,1),left=PageNav.jump(i,count,-1);
    if(count==0){if(right!=0||left!=0)throw new AssertionError("Empty list");continue;}
    if(right<0||right>=count||left<0||left>=count)throw new AssertionError("Bounds "+count);
    if(right%10!=0||left%10!=0)throw new AssertionError("Page boundary");
    if(i/10<(count-1)/10&&right/10!=i/10+1)throw new AssertionError("Next page");
    if(i/10>0&&left/10!=i/10-1)throw new AssertionError("Previous page");
   }
  }
  System.out.println("PASS: 10-channel navigation, empty and partial pages, first/last boundaries");
 }
}
