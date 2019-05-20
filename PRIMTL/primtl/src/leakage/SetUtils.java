/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/* How to use this class:
    SetUtils setA = new SetUtils();
    setA.add("1");
    setA.add("5");
    setA.add("2");
    setA.remove("5");

    SetUtils setB = new SetUtils();
    setB.add("10");
    setB.add("5");
    setB.add("2");
    System.out.println("setA: "+setA);
    System.out.println("setB: "+setB);
    System.out.println("intersection: "+SetUtils.intersection(setA, setB));   
    System.out.println("union: "+SetUtils.union(setA, setB));
    System.out.println("difference A\\B: "+SetUtils.difference(setA, setB));
*/
package leakage;

import java.util.HashSet;
import java.util.Set;


public class SetUtils extends HashSet{
    
    public static <T> Set<T> union(Set<T> setA, Set<T> setB) {
        Set<T> tmp = new HashSet<>(setA);
        tmp.addAll(setB);
        return tmp;
    }

    public static <T> Set<T> intersection(Set<T> setA, Set<T> setB) {
        Set<T> tmp = new HashSet<>();
        for (T x : setA)
          if (setB.contains(x))
            tmp.add(x);
        return tmp;
    }
    
    public static <T> Set<T> intersection(Set<T> setA, Set<T> setB, Set<T> setC) {
        Set<T> tmp = new HashSet<>();
        Set<T> interSec = new HashSet<>();
        tmp = intersection(setA, setB);
        interSec = intersection(tmp, setC);
        return interSec;
    }


    public static <T> Set<T> difference(Set<T> setA, Set<T> setB) {
        Set<T> tmp = new HashSet<>(setA);
        tmp.removeAll(setB);
        return tmp;
    }

    public static <T> boolean isSubset(Set<T> setA, Set<T> setB) {
        return setB.containsAll(setA);
    }

    public static <T> boolean equals(Set<T> setA, Set<T> setB) {
        return setA.containsAll(setB) && setB.containsAll(setA);
    }
}
