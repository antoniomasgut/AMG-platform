terraform {
  required_providers {
    hcloud = {
      source  = "hetznercloud/hcloud"
      version = "~> 1.47"
    }
  }
  required_version = ">= 1.0"

  # Estat remot recomanat per producció
  # backend "s3" {
  #   bucket = "amg-terraform-state"
  #   key    = "infra/terraform.tfstate"
  #   region = "eu-west-1"
  # }
}

provider "hcloud" {
  token = var.hcloud_token
}

# ── Clau SSH ──────────────────────────────────────────────────────────────────
resource "hcloud_ssh_key" "amg_key" {
  name       = "amg-deploy-key"
  public_key = file(var.ssh_public_key_path)
}

# ── Xarxa privada (per comunicació interna entre servidors) ───────────────────
resource "hcloud_network" "amg_net" {
  name     = "amg-network"
  ip_range = "10.0.0.0/16"
}

resource "hcloud_network_subnet" "amg_subnet" {
  network_id   = hcloud_network.amg_net.id
  type         = "cloud"
  network_zone = "eu-central"
  ip_range     = "10.0.1.0/24"
}

# ── Servidor principal (app + tot en un) ─────────────────────────────────────
resource "hcloud_server" "app" {
  name        = "amg-app"
  image       = "ubuntu-24.04"
  server_type = var.app_server_type
  location    = var.location
  ssh_keys    = [hcloud_ssh_key.amg_key.id]

  network {
    network_id = hcloud_network.amg_net.id
    ip         = "10.0.1.10"
  }

  labels = {
    role        = "app"
    environment = "production"
    project     = "amg-digitalitzacio"
  }

  user_data = <<-EOF
    #!/bin/bash
    apt-get update -q
    apt-get install -y docker.io docker-compose-plugin curl git
    systemctl enable docker
    systemctl start docker
    usermod -aG docker ubuntu
  EOF
}

# ── Firewall ──────────────────────────────────────────────────────────────────
resource "hcloud_firewall" "amg_fw" {
  name = "amg-firewall"

  rule {
    direction = "in"
    port      = "22"
    protocol  = "tcp"
    source_ips = ["0.0.0.0/0", "::/0"]
  }

  rule {
    direction = "in"
    port      = "80"
    protocol  = "tcp"
    source_ips = ["0.0.0.0/0", "::/0"]
  }

  rule {
    direction = "in"
    port      = "443"
    protocol  = "tcp"
    source_ips = ["0.0.0.0/0", "::/0"]
  }
}

resource "hcloud_firewall_attachment" "app_fw" {
  firewall_id = hcloud_firewall.amg_fw.id
  server_ids  = [hcloud_server.app.id]
}

# ── Volum persistent per a dades (postgres_data, minio_data) ─────────────────
resource "hcloud_volume" "app_data" {
  name      = "amg-app-data"
  size      = 100  # GB
  server_id = hcloud_server.app.id
  automount = true
  format    = "ext4"
}

# ── Servidor DB dedicat (activar si architecture = "app_db" o "full") ─────────
resource "hcloud_server" "db" {
  count       = var.architecture == "single" ? 0 : 1
  name        = "amg-db"
  image       = "ubuntu-24.04"
  server_type = var.db_server_type
  location    = var.location
  ssh_keys    = [hcloud_ssh_key.amg_key.id]

  network {
    network_id = hcloud_network.amg_net.id
    ip         = "10.0.1.20"
  }

  labels = {
    role    = "database"
    project = "amg-digitalitzacio"
  }

  user_data = <<-EOF
    #!/bin/bash
    apt-get update -q
    apt-get install -y docker.io docker-compose-plugin
    systemctl enable docker
    systemctl start docker
  EOF
}

resource "hcloud_volume" "db_data" {
  count     = var.architecture == "single" ? 0 : 1
  name      = "amg-db-data"
  size      = 50
  server_id = hcloud_server.db[0].id
  automount = true
  format    = "ext4"
}

# ── Servidor n8n dedicat (activar si architecture = "full") ──────────────────
resource "hcloud_server" "n8n" {
  count       = var.architecture == "full" ? 1 : 0
  name        = "amg-n8n"
  image       = "ubuntu-24.04"
  server_type = var.n8n_server_type
  location    = var.location
  ssh_keys    = [hcloud_ssh_key.amg_key.id]

  network {
    network_id = hcloud_network.amg_net.id
    ip         = "10.0.1.30"
  }

  labels = {
    role    = "n8n"
    project = "amg-digitalitzacio"
  }
}
