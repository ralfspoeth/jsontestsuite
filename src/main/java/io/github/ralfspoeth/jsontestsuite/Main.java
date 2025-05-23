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
        return new Result(p, rdr.readElement(),null);
    } catch (Throwable t) {
        return new Result(p, null, t);
    }
}

Predicate<Path> fileNameFilter(FileSystem fs, String pattern) {
    return p -> fs.getPathMatcher("glob:" + pattern).matches(p.getFileName());
}

void collectYs(Path resourceDir) throws IOException {
    try(var files = Files.list(resourceDir)) {
        files.filter(fileNameFilter(resourceDir.getFileSystem(), "y*.json"))
                .map(this::parse)
                .filter(r -> r.exception != null)
                .forEach(rejectedYs::add);
    }
}

void collectNs(Path resourceDir) throws IOException {
    try(var files = Files.list(resourceDir)) {
        files.filter(fileNameFilter(resourceDir.getFileSystem(), "n*.json"))
                .map(this::parse)
                .filter(r -> r.exception == null && r.element!=null)
                .forEach(acceptedNs::add);
    }
}

void collectIs(Path resourceDir) throws IOException {
    try(var files = Files.list(resourceDir)) {
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

void listResults(List<Result> results) {
    System.out.println("Path\tElement\tException");
    results.forEach(r -> System.out.printf("%s\t%s\t%s%n",
            r.p,
            r.element==null?"":r.element.json().replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t"),
            r.exception==null?"":r.exception.getMessage())
    );
}

void main() throws Exception {
    var resourcesDir = Path.of(requireNonNull(getClass().getResource("/")).toURI());
    collectNs(resourcesDir);
    collectYs(resourcesDir);
    collectIs(resourcesDir);
    System.out.printf("Accepted N Files: %d%n", acceptedNs.size());
    listResults(acceptedNs);
    System.out.printf("Rejected Y Files: %d%n", rejectedYs.size());
    listResults(rejectedYs);
    System.out.printf("Accepted I Files: %d%n", acceptedIs.size());
    listResults(acceptedIs);
    System.out.printf("Rejected I Files: %d%n", rejectedIs.size());
    listResults(rejectedIs);
}
