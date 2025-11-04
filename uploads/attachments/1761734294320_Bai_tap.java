/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.bai_tap_genneric;

/**
 *
 * @author HP
 */
public class Bai_tap <K, V> {
    private K key;
    private V value;
    
    public Bai_tap(K key, V value){
        this.key = key;
        this.value = value;
    }

  
    public K getKey() {
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }
    public String toString(){
        return "{" + key +
                ":" + value +"}";
    }
}
