# CPD Distributed Chat System

## 1. Introduction
Este projecto implementa um sistema de chat cliente-servidor em Java SE 21+, com salas de chat, autenticação por token e tolerância a falhas.

## 2. Features Implementadas
- **Virtual Threads** (`Thread.startVirtualThread`) para cada cliente, reduzindo overhead de threads.
- **Gestão de concorrência** com `ReentrantLock` em `activeSessions` e `chatRooms`.
- **Autenticação** via `AUTH <user> <pass>`.
- **Fault tolerance**: cliente tenta reconectar‐se automaticamente e reatacha à mesma sala sem pedir credenciais de novo.
- **Comandos**:
  - `JOIN <room>`: Junta-se a uma sala de chat, caso não exista cria.
  - `LEAVE`: Sai da sala atual.
  - `LIST`: Lista salas disponíveis.
  - `MESSAGE <msg>`: Envia mensagem para a sala atual.
  - `QUIT`: Termina a sessão do cliente.

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
