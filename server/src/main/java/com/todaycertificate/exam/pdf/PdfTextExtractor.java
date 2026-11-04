package com.todaycertificate.exam.pdf;

import com.todaycertificate.common.ApiException;
import com.todaycertificate.common.ErrorCode;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.pdfbox.text.TextPosition;

/**
 * PDF에서 글자만 뽑는다. 읽을 수 없는 PDF는 500이 아니라 PDF_INVALID(400)로 알린다.
 * 기출 PDF는 대부분 2단이라, 페이지를 위→아래로만 읽으면 왼쪽·오른쪽 단의 같은 높이 줄이 섞인다.
 * 그래서 페이지마다 [단을 가로지르는 위쪽 띠 → 왼쪽 단 → 오른쪽 단 → 아래쪽 띠] 순서로 읽는다.
 * 1단 페이지는 대부분의 줄이 가운데를 가로질러서 위·아래 띠가 페이지 거의 전체가 되므로 지금처럼 위→아래로 읽힌다.
 */
public final class PdfTextExtractor {

    private static final int MIN_TEXT_LENGTH = 50;

    private PdfTextExtractor() {
    }

    public static String extract(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                text.append(pageText(document, i));
            }
            if (text.toString().replaceAll("\\s", "").length() < MIN_TEXT_LENGTH) {
                throw invalid("글자를 읽을 수 없는 PDF입니다 (스캔본은 지원하지 않아요)");
            }
            return text.toString();
        } catch (InvalidPasswordException e) {
            throw invalid("암호가 걸린 PDF입니다");
        } catch (IOException e) {
            throw invalid("PDF 파일을 열 수 없습니다");
        }
    }

    private static String pageText(PDDocument document, int index) throws IOException {
        PDPage page = document.getPage(index);
        PDRectangle box = page.getCropBox();
        float width = box.getWidth();
        float height = box.getHeight();
        List<TextPosition> chars = chars(document, index + 1);

        float gutter = gutterX(chars, width);
        float topEnd = 0;
        float bottomStart = height;
        for (TextPosition c : chars) {
            if (c.getX() <= gutter && gutter <= c.getX() + c.getWidth()) { // 단 사이를 가로지르는 글자 (헤더·꼬리말·1단 본문)
                if (c.getY() < height / 2) topEnd = Math.max(topEnd, c.getY() + 1);
                else bottomStart = Math.min(bottomStart, c.getY() - 1);
            }
        }

        PDFTextStripperByArea stripper = new PDFTextStripperByArea();
        stripper.setSortByPosition(true); // 한 영역 안에서는 화면 위치 순서로 읽는다
        List<String> regions = List.of("top", "left", "right", "bottom");
        stripper.addRegion("top", new Rectangle2D.Float(0, 0, width, topEnd));
        stripper.addRegion("left", new Rectangle2D.Float(0, topEnd, gutter, bottomStart - topEnd));
        stripper.addRegion("right", new Rectangle2D.Float(gutter, topEnd, width - gutter, bottomStart - topEnd));
        stripper.addRegion("bottom", new Rectangle2D.Float(0, bottomStart, width, height - bottomStart));
        stripper.extractRegions(page);
        StringBuilder text = new StringBuilder();
        for (String region : regions) {
            text.append(stripper.getTextForRegion(region)).append('\n');
        }
        return text.toString();
    }

    /** 한 페이지의 글자 위치를 모은다. */
    private static List<TextPosition> chars(PDDocument document, int pageNumber) throws IOException {
        List<TextPosition> chars = new ArrayList<>();
        PDFTextStripper collector = new PDFTextStripper() {
            @Override
            protected void processTextPosition(TextPosition text) {
                chars.add(text);
            }
        };
        collector.setStartPage(pageNumber);
        collector.setEndPage(pageNumber);
        collector.getText(document);
        return chars;
    }

    /**
     * 단 사이 여백: 페이지 가운데 30% 안에서 글자가 가장 적게 지나가는 x. 같으면 가운데에 가까운 쪽.
     * shortcut: 2단까지만 본다. 3단 PDF가 나오면 여백을 여러 개 찾도록 바꾼다.
     */
    private static float gutterX(List<TextPosition> chars, float width) {
        int from = Math.round(width * 0.35f);
        int to = Math.round(width * 0.65f);
        int[] covered = new int[to - from + 1];
        for (TextPosition c : chars) {
            int start = Math.max(from, (int) Math.floor(c.getX()));
            int end = Math.min(to, (int) Math.ceil(c.getX() + c.getWidth()));
            for (int x = start; x <= end; x++) covered[x - from]++;
        }
        int best = Math.round(width / 2) - from;
        for (int i = 0; i < covered.length; i++) {
            boolean fewer = covered[i] < covered[best];
            boolean sameButCloser = covered[i] == covered[best] && Math.abs(i + from - width / 2) < Math.abs(best + from - width / 2);
            if (fewer || sameButCloser) best = i;
        }
        return best + from + 0.5f;
    }

    private static ApiException invalid(String reason) {
        return new ApiException(ErrorCode.PDF_INVALID, List.of(reason));
    }
}
