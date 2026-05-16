variable "hcloud_token" {
  description = "Hetzner Cloud API Token"
  type        = string
  sensitive   = true
}

variable "app_domain" {
  description = "Domini principal de l'aplicació"
  type        = string
  default     = "portal.amg.cat"
}

variable "ssh_public_key_path" {
  description = "Ruta de la clau SSH pública per als servidors"
  type        = string
  default     = "~/.ssh/id_rsa.pub"
}

variable "app_server_type" {
  description = "Tipus de servidor per a l'aplicació"
  type        = string
  default     = "cx22"   # 2 vCPU / 4 GB RAM — inici
  # cx32 = 4 vCPU / 8 GB
  # cx42 = 8 vCPU / 16 GB
}

variable "db_server_type" {
  description = "Tipus de servidor per a la base de dades (si és dedicat)"
  type        = string
  default     = "cx22"   # 2 vCPU / 4 GB — suficient per PostgreSQL fins a 50 clients
}

variable "n8n_server_type" {
  description = "Tipus de servidor per a n8n (si és dedicat)"
  type        = string
  default     = "cx22"
}

variable "location" {
  description = "Regió Hetzner"
  type        = string
  default     = "nbg1"   # Nuremberg (Europa central)
  # fsn1 = Falkenstein, hel1 = Helsinki
}

variable "architecture" {
  description = "Arquitectura de desplegament: single | app_db | full"
  type        = string
  default     = "single"
  # single  = tot en un servidor (0-20 clients)
  # app_db  = app + DB separats (20-50 clients)
  # full    = app + DB + n8n separats (50+ clients)
}
