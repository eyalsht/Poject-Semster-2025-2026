package util;

import common.content.City;
import common.report.ActivityReport;
import common.report.AllClientsReport;
import common.report.PurchasesReport;
import common.report.SupportRequestsReport;
import common.support.SupportTicketRowDTO;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.BarChart;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportPdfExporter {

    private static final float MARGIN = 40f;
    private static final float GAP = 12f;

    private static final PDType1Font FONT = PDType1Font.HELVETICA;
    private static final PDType1Font FONT_BOLD = PDType1Font.HELVETICA_BOLD;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ====== IMPORTANT FIX ======
    // In the app, your chart sits on a dark background, so white axis labels are visible.
    // In the PDF, the chart snapshot transparency ends up on a white page => white labels disappear.
    // We DO NOT change label colors. We fill the snapshot background with a dark color.
    private static final Color SNAPSHOT_BG = Color.rgb(12, 12, 12); // dark background behind chart labels

    // ========= PUBLIC API =========

    public static void exportClientsReport(File out,
                                           AllClientsReport report,
                                           BarChart<String, Number> chart) throws IOException {

        try (PDDocument doc = new PDDocument()) {
            PageCtx ctx = newPage(doc);

            float y = drawHeader(doc, ctx,
                    "Clients Report",
                    null,
                    null, null,
                    "Generated: " + LocalDateTime.now().format(DT_FMT));

            y = drawChartImage(doc, ctx, chart, y);

            y -= GAP;
            y = drawTableTitle(doc, ctx, "Clients (newest first)", y);

            String[] headers = {"ID", "Username", "Email", "First name", "Last name", "Created"};
            float[] widths   = {40,   90,        170,     85,          85,         90};

            y = drawTableHeader(doc, ctx, headers, widths, y);

            List<AllClientsReport.ClientRow> rows = report == null ? null : report.clientsNewestFirst;
            if (rows != null) {
                for (AllClientsReport.ClientRow r : rows) {
                    String created = (r.createdAt == null) ? "" : r.createdAt.format(DT_FMT);
                    String[] vals = {
                            String.valueOf(r.userId),
                            safe(r.username),
                            safe(r.email),
                            safe(r.firstName),
                            safe(r.lastName),
                            created
                    };
                    y = drawTableRow(doc, ctx, vals, widths, y);
                }
            }

            doc.save(out);
        }
    }

    public static void exportPurchasesReport(File out,
                                             PurchasesReport report,
                                             BarChart<String, Number> chart) throws IOException {

        try (PDDocument doc = new PDDocument()) {
            PageCtx ctx = newPage(doc);

            String city = (report == null) ? "" : report.cityName;
            LocalDate from = (report == null) ? null : report.from;
            LocalDate to   = (report == null) ? null : report.to;

            float y = drawHeader(doc, ctx,
                    "Purchases Report",
                    city,
                    from, to,
                    "Generated: " + LocalDateTime.now().format(DT_FMT));

            y = drawChartImage(doc, ctx, chart, y);

            y -= 16;
            y = drawTableTitle(doc, ctx, "Summary", y);

            String[] headers = {"One-time", "Subscriptions", "Renewals"};
            float[] widths   = {180,       180,            180};

            y = drawTableHeader(doc, ctx, headers, widths, y);

            if (report != null) {
                String[] vals = {
                        String.valueOf(report.oneTime),
                        String.valueOf(report.subscriptions),
                        String.valueOf(report.renewals)
                };
                y = drawTableRow(doc, ctx, vals, widths, y);
            }

            doc.save(out);
        }
    }

    public static void exportActivityReport(File out,
                                            ActivityReport report,
                                            City selectedCity,
                                            LocalDate from, LocalDate to,
                                            BarChart<String, Number> chart) throws IOException {

        try (PDDocument doc = new PDDocument()) {
            PageCtx ctx = newPage(doc);

            String cityName = (selectedCity == null) ? "" :
                    (selectedCity.getId() == -1 ? "All cities" : selectedCity.getName());

            float y = drawHeader(doc, ctx,
                    "Activity Report",
                    cityName,
                    from, to,
                    "Generated: " + LocalDateTime.now().format(DT_FMT));

            y = drawChartImage(doc, ctx, chart, y);

            y -= GAP;
            int cityEnters = (report == null) ? 0 : report.cityEnterViewsTotal;
            y = drawSmallLine(doc, ctx, y, "City enters (total): " + cityEnters);

            y -= 10;
            y = drawTableTitle(doc, ctx, "Maps activity", y);

            String[] headers = {"Map", "City", "Downloads", "Views"};
            float[] widths   = {260,  140,    70,          70};

            y = drawTableHeader(doc, ctx, headers, widths, y);

            List<ActivityReport.MapRow> rows = (report == null) ? null : report.mapRows;
            if (rows != null) {
                for (ActivityReport.MapRow r : rows) {
                    String[] vals = {
                            safe(r.mapName),
                            safe(r.cityName),
                            String.valueOf(r.downloads),
                            String.valueOf(r.views)
                    };
                    y = drawTableRow(doc, ctx, vals, widths, y);
                }
            }

            doc.save(out);
        }
    }

    public static void exportSupportRequestsReport(File out,
                                                   SupportRequestsReport report,
                                                   BarChart<String, Number> chart) throws IOException {

        try (PDDocument doc = new PDDocument()) {
            PageCtx ctx = newLandscapePage(doc);

            float y = drawHeader(doc, ctx,
                    "Support Requests Report",
                    null,
                    null, null,
                    "Generated: " + LocalDateTime.now().format(DT_FMT));

            y = drawChartImage(doc, ctx, chart, y);

            y -= GAP;
            int pending = (report == null) ? 0 : report.pendingCount;
            int done    = (report == null) ? 0 : report.doneCount;
            y = drawSmallLine(doc, ctx, y, "Pending: " + pending + "   |   Done: " + done);

            y -= 10;
            y = drawTableTitle(doc, ctx, "Tickets (full text included)", y);

            String[] headers = {"Ticket", "Client", "Topic", "Status", "Created", "Client text", "Agent reply"};
            float[] widths   = {45,       80,      110,    65,       95,        250,          250};

            y = drawTableHeader(doc, ctx, headers, widths, y);

            List<SupportTicketRowDTO> rows = (report == null) ? null : report.rows;
            if (rows != null) {
                for (SupportTicketRowDTO r : rows) {
                    String created = (r.getCreatedAt() == null) ? "" : r.getCreatedAt().format(DT_FMT);

                    String[] vals = {
                            String.valueOf(r.getTicketId()),
                            safe(r.getClientUsername()),
                            safe(r.getTopic()),
                            (r.getStatus() == null) ? "" : r.getStatus().name(),
                            created,
                            safe(r.getClientText()),
                            safe(r.getAgentReply())
                    };
                    y = drawTableRow(doc, ctx, vals, widths, y);
                }
            }

            doc.save(out);
        }
    }

    // ========= INTERNALS =========

    private static class PageCtx {
        PDPage page;
        float w, h;
        boolean landscape;
    }

    private static PageCtx newPage(PDDocument doc) {
        PageCtx ctx = new PageCtx();
        ctx.page = new PDPage(PDRectangle.A4);
        doc.addPage(ctx.page);
        ctx.w = ctx.page.getMediaBox().getWidth();
        ctx.h = ctx.page.getMediaBox().getHeight();
        ctx.landscape = false;
        return ctx;
    }

    private static PageCtx newLandscapePage(PDDocument doc) {
        PageCtx ctx = new PageCtx();

        // Create landscape A4 by swapping width/height (works with older PDFBox)
        PDRectangle a4 = PDRectangle.A4;
        PDRectangle landscape = new PDRectangle(a4.getHeight(), a4.getWidth());

        ctx.page = new PDPage(landscape);
        doc.addPage(ctx.page);

        ctx.w = ctx.page.getMediaBox().getWidth();
        ctx.h = ctx.page.getMediaBox().getHeight();
        ctx.landscape = true;
        return ctx;
    }

    private static float drawHeader(PDDocument doc, PageCtx ctx,
                                    String title,
                                    String cityName,
                                    LocalDate from, LocalDate to,
                                    String generatedLine) throws IOException {

        try (PDPageContentStream cs = new PDPageContentStream(doc, ctx.page)) {
            float y = ctx.h - MARGIN;

            cs.setFont(FONT_BOLD, 20);
            y -= 22;
            text(cs, MARGIN, y, title);

            cs.setFont(FONT, 11);
            y -= 18;

            if (cityName != null && !cityName.isBlank()) {
                text(cs, MARGIN, y, "City: " + cityName);
                y -= 14;
            }

            if (from != null && to != null) {
                text(cs, MARGIN, y, "Dates: " + from + " - " + to);
                y -= 14;
            }

            if (generatedLine != null) {
                text(cs, MARGIN, y, generatedLine);
                y -= 12;
            }

            y -= 8;
            cs.moveTo(MARGIN, y);
            cs.lineTo(ctx.w - MARGIN, y);
            cs.stroke();

            return y - 12;
        }
    }

    /**
     * Chart snapshot for PDF:
     * - NO transform (avoids clipping)
     * - SnapshotParameters.fill is DARK so white axis labels remain visible on PDF's white page
     * - Upscale afterwards for quality
     */
    private static float drawChartImage(PDDocument doc, PageCtx ctx,
                                        BarChart<String, Number> chart,
                                        float yTop) throws IOException {
        if (chart == null || !chart.isVisible()) return yTop;

        chart.applyCss();
        chart.layout();

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(SNAPSHOT_BG); // <<< FIX: makes white axis labels visible in PDF

        int w = (int) Math.ceil(Math.max(1, chart.getWidth()));
        int h = (int) Math.ceil(Math.max(1, chart.getHeight()));

        WritableImage fxImg = new WritableImage(w, h);
        fxImg = chart.snapshot(params, fxImg);

        BufferedImage bImg = SwingFXUtils.fromFXImage(fxImg, null);
        if (bImg == null) return yTop;

        BufferedImage hiRes = upscale(bImg, 2.0);
        var pdImage = LosslessFactory.createFromImage(doc, hiRes);

        float maxW = ctx.w - 2 * MARGIN;
        float maxH = 360;

        float imgW = pdImage.getWidth();
        float imgH = pdImage.getHeight();

        float pdfScale = Math.min(maxW / imgW, maxH / imgH);
        float drawW = imgW * pdfScale;
        float drawH = imgH * pdfScale;

        float x = MARGIN + (maxW - drawW) / 2;
        float y = yTop - drawH;

        try (PDPageContentStream cs = new PDPageContentStream(
                doc, ctx.page, PDPageContentStream.AppendMode.APPEND, true)) {
            cs.drawImage(pdImage, x, y, drawW, drawH);
        }

        return y - 18;
    }

    private static float drawTableTitle(PDDocument doc, PageCtx ctx, String title, float y) throws IOException {
        try (PDPageContentStream cs = new PDPageContentStream(doc, ctx.page, PDPageContentStream.AppendMode.APPEND, true)) {
            cs.setFont(FONT_BOLD, 13);
            text(cs, MARGIN, y - 14, title);
        }
        return y - 22;
    }

    private static float drawSmallLine(PDDocument doc, PageCtx ctx, float y, String line) throws IOException {
        try (PDPageContentStream cs = new PDPageContentStream(doc, ctx.page, PDPageContentStream.AppendMode.APPEND, true)) {
            cs.setFont(FONT, 11);
            text(cs, MARGIN, y - 12, line);
        }
        return y - 18;
    }

    private static float drawTableHeader(PDDocument doc, PageCtx ctx,
                                         String[] headers, float[] widths,
                                         float y) throws IOException {
        y = ensureSpace(doc, ctx, y, 30);

        try (PDPageContentStream cs = new PDPageContentStream(doc, ctx.page, PDPageContentStream.AppendMode.APPEND, true)) {
            cs.setFont(FONT_BOLD, 10);

            float x = MARGIN;
            float yText = y - 12;

            for (int i = 0; i < headers.length; i++) {
                String h = headers[i];
                text(cs, x + 2, yText, truncate(h, 60));
                x += widths[i];
            }

            float yLine = y - 16;
            cs.moveTo(MARGIN, yLine);
            cs.lineTo(ctx.w - MARGIN, yLine);
            cs.stroke();
        }

        return y - 20;
    }

    private static float drawTableRow(PDDocument doc, PageCtx ctx,
                                      String[] vals, float[] widths,
                                      float y) throws IOException {

        String[][] wrapped = new String[vals.length][];
        int maxLines = 1;
        for (int i = 0; i < vals.length; i++) {
            float cellW = widths[i] - 6;
            wrapped[i] = wrapText(safe(vals[i]), FONT, 9, cellW);
            maxLines = Math.max(maxLines, wrapped[i].length);
        }

        float rowH = 12 + (maxLines * 10);
        y = ensureSpace(doc, ctx, y, rowH + 10);

        try (PDPageContentStream cs = new PDPageContentStream(doc, ctx.page, PDPageContentStream.AppendMode.APPEND, true)) {
            cs.setFont(FONT, 9);

            float x = MARGIN;
            float baseY = y - 12;

            for (int i = 0; i < vals.length; i++) {
                String[] lines = wrapped[i];
                float lineY = baseY;
                for (String ln : lines) {
                    text(cs, x + 2, lineY, ln);
                    lineY -= 10;
                }
                x += widths[i];
            }

            float yLine = y - rowH;
            cs.moveTo(MARGIN, yLine);
            cs.lineTo(ctx.w - MARGIN, yLine);
            cs.stroke();
        }

        return y - rowH;
    }

    private static float ensureSpace(PDDocument doc, PageCtx ctx, float y, float needed) throws IOException {
        float bottom = MARGIN + 30;
        if (y - needed >= bottom) return y;

        PageCtx newCtx = ctx.landscape ? newLandscapePage(doc) : newPage(doc);
        ctx.page = newCtx.page;
        ctx.w = newCtx.w;
        ctx.h = newCtx.h;
        ctx.landscape = newCtx.landscape;

        return ctx.h - MARGIN;
    }

    private static void text(PDPageContentStream cs, float x, float y, String t) throws IOException {
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(pdfSafe(t));
        cs.endText();
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("\r", " ").replace("\n", " ").trim();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max - 3) + "...";
    }

    private static String[] wrapText(String text, PDType1Font font, float fontSize, float maxWidth) throws IOException {
        if (text == null || text.isBlank()) return new String[]{""};

        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        java.util.List<String> lines = new java.util.ArrayList<>();

        for (String w : words) {
            float wWidth = font.getStringWidth(w) / 1000f * fontSize;
            if (wWidth > maxWidth) {
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                    line = new StringBuilder();
                }
                lines.add(truncate(w, 60));
                continue;
            }

            String candidate = line.isEmpty() ? w : line + " " + w;
            float width = font.getStringWidth(candidate) / 1000f * fontSize;

            if (width <= maxWidth) {
                line = new StringBuilder(candidate);
            } else {
                if (!line.isEmpty()) lines.add(line.toString());
                line = new StringBuilder(w);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines.toArray(new String[0]);
    }

    private static String pdfSafe(String s) {
        if (s == null) return "";
        return s
                .replace('\u2192', '-')   // → arrow
                .replace('\u2013', '-')   // – en dash
                .replace('\u2014', '-')   // — em dash
                .replace('\u2018', '\'')  // ‘
                .replace('\u2019', '\'')  // ’
                .replace('\u201C', '"')   // “
                .replace('\u201D', '"')   // ”
                .replace("\r", " ")
                .replace("\n", " ")
                .trim();
    }

    private static BufferedImage upscale(BufferedImage src, double scale) {
        int newW = (int) Math.ceil(src.getWidth() * scale);
        int newH = (int) Math.ceil(src.getHeight() * scale);

        BufferedImage dst = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = dst.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.drawImage(src, 0, 0, newW, newH, null);
        g2.dispose();
        return dst;
    }
}
