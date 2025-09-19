package com.example.datacapture;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class TableModel {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int no;
    public String salesmanNo;
    public String barcode;
    public String vrpRate;
    public String billNo;
    public String date;
    public String jappa;

    public TableModel(int no, String salesmanNo, String barcode, String vrpRate, String billNo, String date, String jappa) {
        this.no = no;
        this.salesmanNo = salesmanNo;
        this.barcode = barcode;
        this.vrpRate = vrpRate;
        this.billNo = billNo;
        this.date = date;
        this.jappa = jappa;
    }
}
