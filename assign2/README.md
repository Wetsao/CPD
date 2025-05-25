# CPD Distributed Chat System

## 1. Introduction
Este projecto implementa um sistema de chat cliente-servidor em Java SE 21+, com salas de chat, autenticação por token, tolerância a falhas e a possibilidade de comunicação com uma AI.

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
  - `CREATE_AI <name> <prompt>`: Cria uma sala com AI.

## 3. Project Structure
Na pasta `src/`:

- `AiRoom.java`
- `ChatServer.java`
- `ChatClient.java`
- `Room.java`
- `Session.java`
- `users.txt` (credenciais no formato `username:password`)


## 4. Prerequisites
- Java SE 21 ou superior
- Ficheiro `users.txt` com um par `username:password` por linha
- Ollama14


## 5. Compilação & Execução

Abra um terminal na pasta `src/` e execute:

```bash
# 1) Compilar todos os fontes
javac ChatServer.java Room.java ChatClient.java

# 2) Arrancar o servidor (usa porta 12345 por omissão)
java ChatServer

# 3) Noutro terminal, arrancar o cliente
java ChatClient 

# 4) Para utilização da "room" com AI é necessario correr o docker com ollama14, em outro terminal:
sudo docker run -d -v ollama:/root/.ollama -p 11434:11434 --name ollama14 ollama/ollama 

# 5) Em caso do container ja ter sido criado

docker start ollama14

