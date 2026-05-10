import "dotenv/config";
import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import pg from "pg";

const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const inputFile = process.argv[2];

if (!inputFile) {
  console.error("Usage: node scripts/run-sql.mjs sql/schema.sql");
  process.exit(1);
}

const databaseUrl = process.env.DATABASE_URL;

if (!databaseUrl) {
  console.error("DATABASE_URL is missing. Create backend/.env from .env.example first.");
  process.exit(1);
}

async function loadSql(filePath, seen = new Set()) {
  const absolutePath = path.resolve(rootDir, filePath);

  if (seen.has(absolutePath)) {
    throw new Error(`Circular SQL include detected: ${filePath}`);
  }

  seen.add(absolutePath);
  const raw = await readFile(absolutePath, "utf8");
  const lines = raw.split(/\r?\n/);
  const expanded = [];

  for (const line of lines) {
    const trimmed = line.trim();

    if (trimmed.startsWith("\\i ")) {
      const includePath = trimmed.slice(3).trim();
      expanded.push(await loadSql(includePath, seen));
    } else {
      expanded.push(line);
    }
  }

  seen.delete(absolutePath);
  return expanded.join("\n");
}

function splitStatements(sql) {
  return sql
    .split(";")
    .map((statement) => statement.trim())
    .filter(Boolean)
    .map((statement) => `${statement};`);
}

const client = new pg.Client({ connectionString: databaseUrl });

try {
  const sql = await loadSql(inputFile);
  const statements = splitStatements(sql);

  await client.connect();

  for (const statement of statements) {
    await client.query(statement);
  }

  console.log(`Executed ${statements.length} SQL statements from ${inputFile}`);
} catch (error) {
  console.error(error instanceof Error ? error.message : error);
  process.exitCode = 1;
} finally {
  await client.end().catch(() => undefined);
}
