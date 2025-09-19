package com.example.datacapture;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TableAdapter extends RecyclerView.Adapter<TableAdapter.ViewHolder> {
    private List<TableModel> items;

    public TableAdapter(List<TableModel> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.table_row, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TableModel item = items.get(position);
        holder.no.setText(String.valueOf(item.no));
        holder.salesmanNo.setText(item.salesmanNo);
        holder.barcode.setText(item.barcode);
        holder.vrpRate.setText(item.vrpRate);
        holder.billNo.setText(item.billNo);
        holder.date.setText(item.date);
        holder.jappa.setText(item.jappa);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView no, salesmanNo, barcode, vrpRate, billNo, date, jappa;
        ViewHolder(View itemView) {
            super(itemView);
            no = itemView.findViewById(R.id.col_no);
            salesmanNo = itemView.findViewById(R.id.col_salesman);
            barcode = itemView.findViewById(R.id.col_barcode);
            vrpRate = itemView.findViewById(R.id.col_vrprate);
            billNo = itemView.findViewById(R.id.col_billno);
            date = itemView.findViewById(R.id.col_date);
            jappa = itemView.findViewById(R.id.col_jappa);
        }
    }
}
