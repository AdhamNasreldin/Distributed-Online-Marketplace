import type { ShardSchema } from "../types/market.js";

export const shardSchemas: ShardSchema[] = ["shard_0", "shard_1"];

export function shardForKey(value: string): ShardSchema {
  const score = value.split("").reduce((total, char) => total + char.charCodeAt(0), 0);
  return shardSchemas[score % shardSchemas.length];
}

export function assertShardSchema(value: string): ShardSchema {
  if (value === "shard_0" || value === "shard_1") {
    return value;
  }

  throw new Error(`Invalid shard schema: ${value}`);
}
