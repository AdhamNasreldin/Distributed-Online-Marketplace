import type { CsvImportRow } from "../types/market";

const requiredColumns = ["name", "brand", "price", "quantity", "category"];

function splitCsvLine(line: string) {
  const cells: string[] = [];
  let current = "";
  let quoted = false;

  for (let index = 0; index < line.length; index += 1) {
    const char = line[index];
    const next = line[index + 1];

    if (char === '"' && next === '"') {
      current += '"';
      index += 1;
    } else if (char === '"') {
      quoted = !quoted;
    } else if (char === "," && !quoted) {
      cells.push(current.trim());
      current = "";
    } else {
      current += char;
    }
  }

  cells.push(current.trim());
  return cells;
}

export function parseProductCsv(raw: string): CsvImportRow[] {
  const lines = raw
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);

  if (lines.length === 0) {
    return [];
  }

  const headers = splitCsvLine(lines[0]).map((header) => header.toLowerCase());
  const missingColumns = requiredColumns.filter((column) => !headers.includes(column));

  return lines.slice(1).map((line) => {
    const values = splitCsvLine(line);
    const record = Object.fromEntries(headers.map((header, index) => [header, values[index] ?? ""]));
    const price = Number(record.price);
    const quantity = Number(record.quantity);
    const errors: string[] = [];

    missingColumns.forEach((column) => errors.push(`Missing column: ${column}`));
    if (!record.name) errors.push("Name is required");
    if (!record.brand) errors.push("Brand is required");
    if (!record.category) errors.push("Category is required");
    if (!Number.isFinite(price) || price <= 0) errors.push("Price must be a positive number");
    if (!Number.isInteger(quantity) || quantity < 0) errors.push("Quantity must be a whole number");

    return {
      name: record.name ?? "",
      brand: record.brand ?? "",
      price: Number.isFinite(price) ? price : 0,
      quantity: Number.isInteger(quantity) ? quantity : 0,
      category: record.category ?? "",
      description: record.description ?? "",
      valid: errors.length === 0,
      errors
    };
  });
}
