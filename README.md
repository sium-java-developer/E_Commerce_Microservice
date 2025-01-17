Container Deployment Architecture
---------------------------------

   @/
  /| ->Browser --> 8080[frontend network]
  / \                         |
(User)                        |
                              |
                    +--------------------+
                    |  frontend service  |...readOnly...<HTTP configuration>
                    |   "productWeb"    |
                    +--------------------+
                              |
                      [backend network]
                              |
                    +--------------------+
                    |   infra service    |
                    |      "kafka"       |
                    +--------------------+
                              |
                      [backend network]
                              |
                    +--------------------+
                    |  business service  |
                    |  "productServer"  |
                    +--------------------+
                              |
                      [backend network]
                              |
                    +--------------------+
                    |  backend service   | read+write ___________________
                    |  "PostgreSQLDB"    |=======(      volume       )
                    +--------------------+        \_________________/


