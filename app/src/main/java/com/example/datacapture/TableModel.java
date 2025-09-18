package com.example.datacapture;

public class TableModel {
    int no;
    String salesmanNo, barcode, vrpRate, billNo, date, jappa;

    public TableModel(int num, String s, String b, String v, String bill, String d, String j) {
        no = num;
        salesmanNo = s;
        barcode = b;
        vrpRate = v;
        billNo = bill;
        date = d;
        jappa = j;
    }
}
