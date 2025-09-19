package com.datacapture;

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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;

public class OCRManager {
    public interface OCRResultListener {
        void onOCRResult(List<TableModel> results);
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

        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
            .addOnSuccessListener(text -> barcodeScanner.process(image)
                .addOnSuccessListener(barcodes -> listener.onOCRResult(parseData(text, barcodes)))
            );
    }

    private List<TableModel> parseData(Text text, List<Barcode> barcodes) {
        List<TableModel> results = new ArrayList<>();
        String dateRegex = "\\d{2}/\\d{2}/\\d{4}";
        Pattern dateP = Pattern.compile(dateRegex);

        List<String> textBlocks = new ArrayList<>();
        for (Text.TextBlock block : text.getTextBlocks()) {
            textBlocks.add(block.getText());
        }
        String allText = String.join("\n", textBlocks);
        Matcher dateMatcher = dateP.matcher(allText);

        String date = dateMatcher.find() ? dateMatcher.group() : "";
        String barcodeNum = !barcodes.isEmpty() ? barcodes.get(0).getDisplayValue() : "";

        String salesmanNo = "";
        Matcher smMatcher = Pattern.compile("\\d{3}").matcher(allText);
        if (smMatcher.find()) salesmanNo = smMatcher.group();

        Matcher billMatcher = Pattern.compile("Bill No.?[:]? ?(\\d+)").matcher(allText);
        String billNo = billMatcher.find() ? billMatcher.group(1) : "";

        Matcher vrpMatcher = Pattern.compile("VRP Rate[:]? ?(\\d+)").matcher(allText);
        String vrpRate = vrpMatcher.find() ? vrpMatcher.group(1) : "";

        Matcher jappaMatcher = Pattern.compile("E[:]? ?(\\d+)").matcher(allText); // adjust E logic if needed
        String jappa = jappaMatcher.find() ? jappaMatcher.group(1) : "";

        TableModel row = new TableModel(results.size() + 1, salesmanNo, barcodeNum, vrpRate, billNo, date, jappa);
        results.add(row);
        return results;
    }
}
