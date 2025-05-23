import io.github.ralfspoeth.json.Element;
import io.github.ralfspoeth.json.io.JsonReader;

import static java.util.Objects.requireNonNull;

record Result(Path p, Element element, Throwable exception) {}

private final List<Result> acceptedNs = new ArrayList<>();
private final List<Result> rejectedYs = new ArrayList<>();
private final List<Result> acceptedIs = new ArrayList<>();
private final List<Result> rejectedIs = new ArrayList<>();

// parse a single JSON file
Result parse(Path p) {
    try (var rdr = new JsonReader(Files.newBufferedReader(p, StandardCharsets.UTF_8))) {
        return new Result(p, rdr.readElement(), null);
    } catch (Throwable t) {
        return new Result(p, null, t);
    }
}

Predicate<Path> fileNameFilter(FileSystem fs, String pattern) {
    return p -> fs.getPathMatcher("glob:" + pattern).matches(p.getFileName());
}

void collectYs(Path resourceDir) throws IOException {
    try (var files = Files.list(resourceDir)) {
        files.filter(fileNameFilter(resourceDir.getFileSystem(), "y*.json"))
                .map(this::parse)
                .filter(r -> r.exception != null)
                .forEach(rejectedYs::add);
    }
}

void collectNs(Path resourceDir) throws IOException {
    try (var files = Files.list(resourceDir)) {
        files.filter(fileNameFilter(resourceDir.getFileSystem(), "n*.json"))
                .map(this::parse)
                .filter(r -> r.exception == null && r.element != null)
                .forEach(acceptedNs::add);
    }
}

void collectIs(Path resourceDir) throws IOException {
    try (var files = Files.list(resourceDir)) {
        files.filter(fileNameFilter(resourceDir.getFileSystem(), "i*.json"))
                .map(this::parse)
                .forEach(r -> {
                    if (r.exception == null) {
                        acceptedIs.add(r);
                    } else {
                        rejectedIs.add(r);
                    }
                });
    }
}

String maxLength(String s, int max) {
    return max > s.length() ? s : s.substring(0, max-3)+"...";
}

String formatJson(String json) {
    return maxLength(json.replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t"), 40);
}

void listResults(List<Result> results, String should, String did) {
    results.forEach(r -> System.out.printf("%s\t%s\t%s\t%s\t%s%n",
            r.p,
            should,
            did,
            r.element == null ? "" : formatJson(r.element.json()),
            r.exception == null ? "" : r.exception)
    );
}

void main() throws Exception {
    var resourcesDir = Path.of(requireNonNull(getClass().getResource("/")).toURI());
    parse(resourcesDir.resolve("i_string_iso_latin_1.json"));
    collectNs(resourcesDir);
    collectYs(resourcesDir);
    collectIs(resourcesDir);
    System.out.println("Path\tShould accept\tDid accept\tElement\tException");
    listResults(acceptedNs, "no", "yes");
    listResults(rejectedYs, "yes", "no");
    listResults(acceptedIs, "may", "yes");
    listResults(rejectedIs, "may", "no");
}
