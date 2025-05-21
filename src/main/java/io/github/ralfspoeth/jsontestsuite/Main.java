import io.github.ralfspoeth.json.Element;
import io.github.ralfspoeth.json.io.JsonReader;

import static java.util.Objects.requireNonNull;

Element parse(Path p) throws Exception {
    try (var rdr = new JsonReader(Files.newBufferedReader(p))) {
        return rdr.readElement();
    }
}

void main() throws Exception {
    var resourcesDir = Path.of(requireNonNull(getClass().getResource("/")).toURI());
    var jsonMatcher = resourcesDir.getFileSystem().getPathMatcher("glob:*.json");
    var yMatcher = resourcesDir.getFileSystem().getPathMatcher("glob:y*.json");
    var nMatcher = resourcesDir.getFileSystem().getPathMatcher("glob:n*.json");
    var iMatcher = resourcesDir.getFileSystem().getPathMatcher("glob:i*.json");
    record Result(Path p, Element element, Throwable exception) {}
    var results = new ArrayList<Result>();
    try (var sources = Files.list(resourcesDir)) {
        sources.filter(f -> jsonMatcher.matches(f.getFileName()))
                .forEach(p -> {
                    try {
                        var result = parse(p);
                        if (nMatcher.matches(p.getFileName())) {
                            results.add(new Result(p, result, null));
                            //System.err.printf("%s shouldn't have been parsed into %s%n", p.getFileName(), result);
                        } else {
                            //System.out.printf("%s successfully parsed into %s%n", p.getFileName(), result);
                        }
                    } catch (Throwable t) {
                        if (yMatcher.matches(p.getFileName())) {
                            results.add(new Result(p, null, t));
                            //System.err.printf("%s should haven been successfully parsed; exception is: %s%n", p.getFileName(), t);
                        } else if (iMatcher.matches(p.getFileName())) {
                            //System.out.printf("Acceptable not to have parsed %s, reason being %s%n", p.getFileName(), t);
                        } else {
                            //System.out.printf("Good not to have parsed %s, reason being %s%n", p.getFileName(), t);
                        }
                    }
                });
        results.forEach(System.out::println);
    }
}
