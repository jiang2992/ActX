// ============================================================
// 1. Constraints
// ============================================================

CREATE CONSTRAINT business_domain_id_unique IF NOT EXISTS
FOR (n:BusinessDomain)
REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT business_concept_id_unique IF NOT EXISTS
FOR (n:BusinessConcept)
REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT business_term_id_unique IF NOT EXISTS
FOR (n:BusinessTerm)
REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT business_process_id_unique IF NOT EXISTS
FOR (n:BusinessProcess)
REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT business_rule_id_unique IF NOT EXISTS
FOR (n:BusinessRule)
REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT metric_id_unique IF NOT EXISTS
FOR (n:Metric)
REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT data_source_id_unique IF NOT EXISTS
FOR (n:DataSource)
REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT database_id_unique IF NOT EXISTS
FOR (n:Database)
REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT schema_id_unique IF NOT EXISTS
FOR (n:Schema)
REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT table_id_unique IF NOT EXISTS
FOR (n:Table)
REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT column_id_unique IF NOT EXISTS
FOR (n:Column)
REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT knowledge_claim_id_unique IF NOT EXISTS
FOR (n:KnowledgeClaim)
REQUIRE n.id IS UNIQUE;


// ============================================================
// 2. Normal Indexes
// ============================================================

CREATE INDEX business_domain_name_index IF NOT EXISTS
FOR (n:BusinessDomain)
ON (n.name);

CREATE INDEX business_concept_name_index IF NOT EXISTS
FOR (n:BusinessConcept)
ON (n.name);

CREATE INDEX business_term_term_index IF NOT EXISTS
FOR (n:BusinessTerm)
ON (n.term);

CREATE INDEX business_process_name_index IF NOT EXISTS
FOR (n:BusinessProcess)
ON (n.name);

CREATE INDEX business_rule_name_index IF NOT EXISTS
FOR (n:BusinessRule)
ON (n.name);

CREATE INDEX metric_name_index IF NOT EXISTS
FOR (n:Metric)
ON (n.name);

CREATE INDEX data_source_name_index IF NOT EXISTS
FOR (n:DataSource)
ON (n.name);

CREATE INDEX database_name_index IF NOT EXISTS
FOR (n:Database)
ON (n.name);

CREATE INDEX schema_name_index IF NOT EXISTS
FOR (n:Schema)
ON (n.name);

CREATE INDEX table_name_index IF NOT EXISTS
FOR (n:Table)
ON (n.name);

CREATE INDEX column_name_index IF NOT EXISTS
FOR (n:Column)
ON (n.name);

CREATE INDEX knowledge_claim_status_index IF NOT EXISTS
FOR (n:KnowledgeClaim)
ON (n.status);


// ============================================================
// 3. Vector Index
// ============================================================

CREATE VECTOR INDEX knowledge_embedding_index IF NOT EXISTS
FOR (n:Knowledge)
ON n.embedding
OPTIONS {
    indexConfig: {
        `vector.dimensions`: 1536,
        `vector.similarity_function`: 'cosine'
    }
};