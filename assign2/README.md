# CPD Distributed Chat System

## 1. Introduction
Este projecto implementa um sistema de chat cliente-servidor em Java SE 21+, com salas de chat, autenticação por token e tolerância a falhas.

## 2. Features Implementadas
- **Virtual Threads** (`Thread.startVirtualThread`) para cada cliente, reduzindo overhead de threads.
- **Gestão de concorrência** com `ReentrantLock` em `activeSessions` e `chatRooms`.
- **Autenticação** via `AUTH <user> <pass>` (gera token UUID com 30 min de validade).
- **Retoma de sessão** via `AUTH_TOKEN <token>`, com token guardado pelo cliente em `token.txt`.
- **Fault tolerance**: cliente tenta reconectar‐se automaticamente e reatacha à mesma sala sem pedir credenciais de novo.
- Todas as funcionalidades descritas no enunciado estão completas.

## 3. Project Structure
Na pasta `src/`:
- `ChatServer.java`
- `ChatClient.java`
- `Room.java`
- `Session.java`
- `users.txt` (credenciais no formato `username:password`)


## 4. Prerequisites
- Java SE 21 ou superior
- Ficheiro `users.txt` com um par `username:password` por linha


## 5. Compilação & Execução

Abra um terminal na pasta `src/` e execute:

```bash
# 1) Compilar todos os fontes
javac ChatServer.java Room.java ChatClient.java

# 2) Arrancar o servidor (usa porta 12345 por omissão)
java ChatServer

# 3) Noutro terminal, arrancar o cliente
java ChatClient 
