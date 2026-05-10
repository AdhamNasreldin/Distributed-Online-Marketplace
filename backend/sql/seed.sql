insert into shard_0.users (id, full_name, email, password_hash, balance, two_factor_enabled, created_at)
values
  ('u-100', 'Youssef Adel', 'youssef@newera.local', 'plain:demo1234', 18650, true, '2026-04-10T09:00:00Z'),
  ('u-300', 'Store Partner Cairo', 'partner@store.local', 'plain:demo1234', 9000, false, '2026-04-18T17:15:00Z')
on conflict (id) do nothing;

insert into shard_1.users (id, full_name, email, password_hash, balance, two_factor_enabled, created_at)
values
  ('u-200', 'Nour Hassan', 'nour@newera.local', 'plain:demo1234', 4300, true, '2026-04-12T13:30:00Z')
on conflict (id) do nothing;

insert into shard_0.products (id, owner_id, name, brand, category, description, price, quantity, condition, status, listed_at, sold_count, color)
values
  ('p-1004', 'u-100', 'Magic Keyboard', 'Apple', 'Accessories', 'Arabic-English layout keyboard for iPad Pro 11 inch.', 9800, 4, 'New', 'listed', '2026-04-29T08:10:00Z', 4, '#b45309'),
  ('p-1006', 'u-100', 'Ergo MX Mouse', 'Logitech', 'Accessories', 'Wireless ergonomic mouse with USB receiver and Bluetooth pairing.', 3600, 2, 'New', 'listed', '2026-05-05T12:26:00Z', 1, '#2563eb'),
  ('p-1002', 'u-300', 'ThinkPad X1 Carbon', 'Lenovo', 'Laptops', 'Business ultrabook, i7 processor, 16 GB RAM, 512 GB SSD, excellent battery.', 41000, 1, 'Used', 'listed', '2026-05-02T15:12:00Z', 3, '#334155'),
  ('p-1005', 'u-300', 'EOS M50 Mark II', 'Canon', 'Cameras', 'Mirrorless camera with kit lens, two batteries, and travel bag.', 27200, 1, 'Used', 'listed', '2026-05-04T19:05:00Z', 0, '#be123c')
on conflict (id) do nothing;

insert into shard_1.products (id, owner_id, name, brand, category, description, price, quantity, condition, status, listed_at, sold_count, color)
values
  ('p-1001', 'u-200', 'Galaxy Tab S9', 'Samsung', 'Tablets', 'AMOLED Android tablet with keyboard cover, S Pen, and original charger.', 22500, 2, 'Like New', 'listed', '2026-05-01T10:20:00Z', 1, '#0f766e'),
  ('p-1003', 'u-200', 'WH-1000XM5 Headphones', 'Sony', 'Audio', 'Noise cancelling headphones with carrying case and Type-C cable.', 14200, 3, 'Like New', 'listed', '2026-05-03T11:48:00Z', 2, '#7c3aed')
on conflict (id) do nothing;

insert into core.purchases (id, product_id, product_name, buyer_id, seller_id, amount, purchased_at, status)
values
  ('ord-501', 'p-1004', 'Magic Keyboard', 'u-200', 'u-100', 9800, '2026-05-06T14:40:00Z', 'completed'),
  ('ord-502', 'p-1006', 'Ergo MX Mouse', 'u-300', 'u-100', 3600, '2026-05-07T09:05:00Z', 'completed')
on conflict (id) do nothing;

insert into core.transactions (id, type, amount, from_user_id, to_user_id, product_id, description, created_at, status)
values
  ('tx-9001', 'deposit', 8000, null, 'u-100', null, 'Manual verified wallet deposit', '2026-05-03T09:00:00Z', 'completed'),
  ('tx-9002', 'sale', 9800, 'u-200', 'u-100', 'p-1004', 'Sold Magic Keyboard to Nour Hassan', '2026-05-06T14:40:00Z', 'completed'),
  ('tx-9003', 'sale', 3600, 'u-300', 'u-100', 'p-1006', 'Sold Ergo MX Mouse to Store Partner Cairo', '2026-05-07T09:05:00Z', 'completed')
on conflict (id) do nothing;
