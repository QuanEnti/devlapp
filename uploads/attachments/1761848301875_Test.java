/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.bai_tap_genneric;

/**
 *
 * @author HP
 */
public class Test {
    public static void main(String[] args) {
        Bai_tap<String,String> d1 = new Bai_tap<String, String>("hello", "xin chào");
        System.out.println(d1.toString());
        Bai_tap<String, String>[] tuDienAnhViet = new Bai_tap[10];
        tuDienAnhViet[0] = new Bai_tap<String, String>("hello", "xin chào");
        tuDienAnhViet[1] = new Bai_tap<String, String>("hi", "xin chào");
        tuDienAnhViet[2] = new Bai_tap<String, String>("code", "mã nguồn");
        tuDienAnhViet[3] = new Bai_tap<String, String>("dictionary", "từ điển");
        
        for(int i = 0; i<4;i++){
            System.out.println(tuDienAnhViet[i].toString());
        }
         
        Bai_tap<Integer, Character>[] bangMaAscii = new Bai_tap[256];
        bangMaAscii[0] = new Bai_tap<>(32, ' ');
        bangMaAscii[1] = new Bai_tap<>(33, '!');
        
        Bai_tap<Character, Integer>[] bangMaAscii2 = new Bai_tap[256];
        bangMaAscii2[0] = new Bai_tap<>( ' ', 32);
        bangMaAscii2[1] = new Bai_tap<>('!', 33);

    }
}
