package main.java.tools;


public class MergeSortFast {

    static void mergesort(Tuple3<Integer, String, float[]>[] a) {
        if(a.length < 2)                    // if size < 2 return
            return;
        Tuple3<Integer, String, float[]>[] b = new Tuple3[a.length];
        MergeSortAtoA(a, b, 0, a.length);
    }

    static void MergeSortAtoA(Tuple3<Integer, String, float[]>[] a, Tuple3<Integer, String, float[]>[] b, int ll, int ee)
    {
        if(ee - ll > 1) {
            int rr = (ll + ee)>>1;          // midpoint, start of right half
            MergeSortAtoB(a, b, ll, rr);
            MergeSortAtoB(a, b, rr, ee);
            Merge(b, a, ll, rr, ee);        // merge b to a
        }
    }

    static void MergeSortAtoB(Tuple3<Integer, String, float[]>[] a, Tuple3<Integer, String, float[]>[] b, int ll, int ee)
    {
        if(ee - ll > 1) {
            int rr = (ll + ee)>>1;          //midpoint, start of right half
            MergeSortAtoA(a, b, ll, rr);
            MergeSortAtoA(a, b, rr, ee);
            Merge(a, b, ll, rr, ee);        // merge a to b
        } else if((ee - ll) == 1) {         // if just one element
            b[ll] = a[ll];                  //   copy a to b
        }
    }

    static void Merge(Tuple3<Integer, String, float[]>[] a, Tuple3<Integer, String, float[]>[] b, int ll, int rr, int ee) {
        int o = ll;                         // b[]       index
        int l = ll;                         // a[] left  index
        int r = rr;                         // a[] right index
        while(true){                        // merge data
            if(a[l].y.compareToIgnoreCase(a[r].y) < 0){               // if a[l] <= a[r]
                b[o++] = a[l++];            //   copy a[l]
                if(l < rr)                  //   if not end of left run
                    continue;               //     continue (back to while)
                while(r < ee){              //   else copy rest of right run
                    b[o++] = a[r++];
                }
                break;                      //     and return
            } else {                        // else a[l] > a[r]
                b[o++] = a[r++];            //   copy a[r]
                if(r < ee)                  //   if not end of right run
                    continue;               //     continue (back to while)
                while(l < rr){              //   else copy rest of left run
                    b[o++] = a[l++];
                }
                break;                      //     and return
            }
        }
    }

    static int GetPassCount(int n)          // return # passes
    {
        int i = 0;
        for(int s = 1; s < n; s <<= 1)
            i += 1;
        return(i);
    }

}
