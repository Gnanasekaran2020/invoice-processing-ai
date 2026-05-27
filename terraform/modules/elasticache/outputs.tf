output "redis_endpoint" { value = aws_elasticache_cluster.main.cache_nodes[0].address; sensitive = true }
output "cluster_id"     { value = aws_elasticache_cluster.main.cluster_id }
output "redis_port"     { value = aws_elasticache_cluster.main.port }

