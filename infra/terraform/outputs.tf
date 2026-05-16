output "app_server_ip" {
  description = "IP pública del servidor principal"
  value       = hcloud_server.app.ipv4_address
}

output "app_server_private_ip" {
  description = "IP privada del servidor principal (xarxa interna)"
  value       = "10.0.1.10"
}

output "db_server_ip" {
  description = "IP pública del servidor DB (si existeix)"
  value       = length(hcloud_server.db) > 0 ? hcloud_server.db[0].ipv4_address : "N/A (single server mode)"
}

output "db_private_ip" {
  description = "IP privada per a la connexió DB interna"
  value       = length(hcloud_server.db) > 0 ? "10.0.1.20" : "postgres (local Docker)"
}

output "n8n_server_ip" {
  description = "IP pública del servidor n8n (si existeix)"
  value       = length(hcloud_server.n8n) > 0 ? hcloud_server.n8n[0].ipv4_address : "N/A"
}

output "datasource_url" {
  description = "SPRING_DATASOURCE_URL per configurar al backend"
  value = length(hcloud_server.db) > 0 \
    ? "jdbc:postgresql://10.0.1.20:5432/amg_platform" \
    : "jdbc:postgresql://postgres:5432/amg_platform"
}

output "dns_records_needed" {
  description = "Registres DNS a configurar al teu proveïdor"
  value = {
    "A ${var.app_domain}"         = hcloud_server.app.ipv4_address
    "A api.${var.app_domain}"     = hcloud_server.app.ipv4_address
    "A monitor.${var.app_domain}" = hcloud_server.app.ipv4_address
    "A n8n.${var.app_domain}"     = hcloud_server.app.ipv4_address
  }
}
