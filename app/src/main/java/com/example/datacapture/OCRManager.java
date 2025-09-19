package com.example.datacapture;

import android.content.Context;
import android.graphics.Bitmap;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.List;
import java.util.regex.*;

public class OCRManager {
    public interface OCRResultListener {
        void onOCRResult(TableModel result);
    }

    private Context context;
    private OCRResultListener listener;
    private BarcodeScanner barcodeScanner;

    public OCRManager(Context ctx, OCRResultListener l) {
        context = ctx;
        listener = l;
        barcodeScanner = BarcodeScanning.getClient(new BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build());
    }

    public void processImage(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            .process(image)
            .addOnSuccessListener(text -> barcodeScanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    TableModel result = parseData(text, barcodes);
                    listener.onOCRResult(result);
                })
            );
    }

    private TableModel parseData(Text text, List<Barcode> barcodes) {
        String allText = text.getText();
        String salesmanNo = findFirstMatch(allText, "(?<!\\d)327(?!\\d)", "");
        String barcodeNum = !barcodes.isEmpty() ? barcodes.get(0).getDisplayValue() : "";
        String vrpRate = findFirstMatch(allText, "VRP Rate.*?([0-9,]+)[/\\-]", "");
        String billNo = findFirstMatch(allText, "Bill No.?[:]? ?(\\d+)", "");
        String date = findFirstMatch(allText, "\\d{2}/\\d{2}/\\d{4}", "");
        String jappa = findFirstMatch(allText, "E[:]? ?(\\d+)", "");
        return new TableModel(0, salesmanNo, barcodeNum, vrpRate, billNo, date, jappa);
    }

    private String findFirstMatch(String input, String regex, String fallback) {
        Matcher m = Pattern.compile(regex).matcher(input);
        return m.find() ? m.group(1) != null ? m.group(1) : m.group() : fallback;
    }
}
