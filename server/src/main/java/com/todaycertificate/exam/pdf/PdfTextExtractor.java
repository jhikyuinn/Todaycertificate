package com.todaycertificate.exam.pdf;

import com.todaycertificate.common.ApiException;
import com.todaycertificate.common.ErrorCode;
import java.io.IOException;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;

/** PDF에서 글자만 뽑는다. 읽을 수 없는 PDF는 500이 아니라 PDF_INVALID(400)로 알린다. */
public final class PdfTextExtractor {

    private static final int MIN_TEXT_LENGTH = 50;

    private PdfTextExtractor() {
    }

    public static String extract(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // 파일 내부 저장 순서가 아니라 화면 위치 순서로 읽는다
            String text = stripper.getText(document);
            if (text.replaceAll("\\s", "").length() < MIN_TEXT_LENGTH) {
                throw invalid("글자를 읽을 수 없는 PDF입니다 (스캔본은 지원하지 않아요)");
            }
            return text;
        } catch (InvalidPasswordException e) {
            throw invalid("암호가 걸린 PDF입니다");
        } catch (IOException e) {
            throw invalid("PDF 파일을 열 수 없습니다");
        }
    }

    private static ApiException invalid(String reason) {
        return new ApiException(ErrorCode.PDF_INVALID, List.of(reason));
    }
}
