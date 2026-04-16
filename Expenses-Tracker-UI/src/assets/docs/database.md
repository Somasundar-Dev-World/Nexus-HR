# Masterclass: PostgreSQL & The EAV Pattern via JSONB

Most apps use a strict Relational Schema (e.g. Table: \`Employees\`, Columns: \`Name, Age, Department\`). This is highly performant but inflexible.

Nexus-HR provides an "OmniTracker"—a feature where Users define their *own* forms. A user could track "Calories", "Stock Prices", or "Mileage". It is impossible to dynamically create a new PostgreSQL table every time a user makes a form.

To solve this we use the **Entity-Attribute-Value (EAV)** pattern via specialized PostgreSQL constructs.

## 1. The Concept

Instead of a table with columns for every possible datapoint, we have a generic \`TrackerEntry\` table.

| ID | Tracker_ID | Log_Date | Payload (JSON) |
|---|---|---|---|
| 1 | 55 (Fitness) | 2026-04-10 | \`{"pushups": 50, "miles": 2.5}\` |
| 2 | 12 (Finance) | 2026-04-11 | \`{"cost": 49.99, "merchant": "Amazon"}\` |

## 2. Implementation in JPA (Hibernate)

We cannot map a dynamic JSON payload to strict Java fields trivially. We use an \`@Convert\` annotation to marshal data.

\`\`\`java
@Entity
public class TrackerEntry {
    @Id
    private Long id;

    // Use a custom compiler to translate Java Maps to Database Strings
    @Convert(converter = JpaMapConverter.class) 
    @Column(length = 2000)
    private Map<String, Object> fieldValues;
}
\`\`\`

**The Converter class:**
\`\`\`java
@Converter
public class JpaMapConverter implements AttributeConverter<Map<String, Object>, String> {
    private final ObjectMapper mapper = new ObjectMapper();

    // Java -> Postgres
    public String convertToDatabaseColumn(Map<String, Object> meta) {
        return mapper.writeValueAsString(meta); 
    }

    // Postgres -> Java
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        return mapper.readValue(dbData, new TypeReference<Map<String, Object>>() {});
    }
}
\`\`\`

## 3. Querying JSON Constraints

Because we store data in JSON strings (or JSONB in optimized Postgres environments), we can no longer do simple SQL like:
\`SELECT * FROM Entries WHERE cost > 50\`

To query, aggregate, and sum data for the Intelligence Reports, the backend must load the Entries into memory, parse the Maps, and execute Java stream aggregations:
\`\`\`java
// Stream Aggregation in Java
double totalCost = entries.stream()
    .mapToDouble(e -> Double.parseDouble(e.getFieldValues().get("cost").toString()))
    .sum();
\`\`\`
*Note: For enterprise scale (>1M rows), this architecture would require migrating the \`@Column\` explicitly to Postgres \`JSONB\` datatype and issuing native JSON-index queries (\`WHERE payload->>'cost' > '50'\`).*
