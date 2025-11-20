package org.ideplugins.ci_pipeline_lint.gitlab;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FetchGitlabVariables {
    private static final String URL = "https://gitlab.com/gitlab-org/gitlab/-/raw/master/doc/ci/variables/predefined_variables.md";

    public static void main(String[] args) {
        try {
            FetchGitlabVariables parser = new FetchGitlabVariables();
            String markdownContent = parser.fetchMarkdownContent();
            List<Variable> variables = parser.parseMarkdownTables(markdownContent);
            parser.writeToJsonFile(variables, args.length > 0 ? args[0] : "gitlab_variables.json");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private String fetchMarkdownContent() throws IOException {
        String result = "";
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(URL)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch content: " + response);
            }
            if (response.body() != null) {
                result = response.body().string();
            }
        }
        return result;
    }

    private List<Variable> parseMarkdownTables(String content) {
        List<Variable> variables = new ArrayList<>();

        // Pattern to match markdown tables
        Pattern tablePattern = Pattern.compile(
                "\\|([^\\n]+)\\|\\s*\\n\\|([^\\n]+)\\|\\s*\\n((?:\\|[^\\n]+\\|\\s*\\n?)+)",
                Pattern.MULTILINE
        );

        Matcher tableMatcher = tablePattern.matcher(content);

        while (tableMatcher.find()) {
            String headerRow = tableMatcher.group(1).trim();
            String[] headers = parseTableRow(headerRow);

            // Find column indices
            int variableIndex = findColumnIndex(headers, "Variable");
            int availabilityIndex = findColumnIndex(headers, "Availability");
            int descriptionIndex = findColumnIndex(headers, "Description");

            // Skip if Variable column is not found
            if (variableIndex == -1) {
                continue;
            }

            String tableRows = tableMatcher.group(3);
            String[] rows = tableRows.split("\\n");

            for (String row : rows) {
                if (row.trim().isEmpty() || !row.contains("|")) {
                    continue;
                }

                String[] cells = parseTableRow(row.trim());
                if (cells.length > variableIndex && !cells[variableIndex].trim().isEmpty()) {
                    String variable = cleanCell(cells[variableIndex]);
                    String availability = availabilityIndex != -1 && availabilityIndex < cells.length
                            ? cleanCell(cells[availabilityIndex]) : "";
                    String description = descriptionIndex != -1 && descriptionIndex < cells.length
                            ? cleanCell(cells[descriptionIndex]) : "";
                    if (variable.startsWith("CI_MERGE_REQUEST_")){
                        availability = "Merge request pipelines";
                    }
                    if (variable.startsWith("CI_EXTERNAL_PULL_")){
                        availability = "External pull request pipelines";
                    }

                    // Skip header separator rows and empty variables
                    if (!variable.matches("[-:]+") && !variable.trim().isEmpty()) {
                        variables.add(new Variable(variable, availability, description));
                    }
                }
            }
        }

        return variables;
    }

    private String[] parseTableRow(String row) {
        // Remove leading and trailing pipes, then split by pipe
        String cleanRow = row.replaceAll("^\\|", "").replaceAll("\\|$", "");
        return cleanRow.split("\\|");
    }

    private int findColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().toLowerCase().contains(columnName.toLowerCase())) {
                return i;
            }
        }
        return -1;
    }

    private String cleanCell(String cell) {
        if (cell == null) return "";

        // Remove markdown formatting and clean up
        return cell.trim()
                .replaceAll("\\*\\*(.*?)\\*\\*", "$1") // Remove bold
                .replaceAll("\\*(.*?)\\*", "$1")       // Remove italic
                .replaceAll("`(.*?)`", "$1")           // Remove code blocks
                .replaceAll("\\[(.*?)]\\([^)]+\\)", "$1") // Remove links but keep text
                .replaceAll("\\s+", " ")               // Normalize whitespace
                .trim();
    }

    private void writeToJsonFile(List<Variable> variables, String path) throws IOException {
        JsonArray jsonArray = new JsonArray();
        variables.forEach(x->{
            JsonObject object = new JsonObject();
            object.addProperty("Variable", x.variable());
            object.addProperty("Availability", x.availability());
            object.addProperty("Description", x.description());
            jsonArray.add(object);
        });
        Files.writeString(Path.of(path), jsonArray.toString());
    }

    // Record to represent a variable
    public record Variable(String variable, String availability, String description) {
        public Variable {
            // Compact constructor with validation/normalization
            availability = availability != null ? availability : "";
            description = description != null ? description : "";
        }
    }

}
