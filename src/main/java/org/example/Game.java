package org.example;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Game {
    private final int boardSize;
    private final char[][] board;
    private char move;
    String player1;
    String player2;
    String currentPlayer;
    List<Cell> moveHistory;

    private enum GameState {
        RUNNING,
        DRAW,
        X_WIN,
        ZERO_WIN
    }

    private static class Cell {
        public int row;
        public int column;

        public Cell(int row, int column) {
            this.row = row;
            this.column = column;
        }
    }

    public Game(int boardSize) {
        this.boardSize = boardSize;
        board = new char[boardSize][boardSize];
        move = 'X';
        moveHistory = new ArrayList<>();

        for (int row = 0; row < boardSize; ++row) {
            for (int column = 0; column < boardSize; ++column) {
                board[row][column] = ' ';
            }
        }
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        Pattern commandPattern = Pattern.compile("(start (user|easy|medium|hard) (user|easy|medium|hard))|(exit)");

        while (true) {
            System.out.println("Input command:");
            String userInput = scanner.nextLine();
            Matcher commandMatcher = commandPattern.matcher(userInput);

            if (!commandMatcher.matches()) {
                System.out.println("Bad parameters!");
                continue;
            }

            Scanner commandScanner = new Scanner(userInput);
            String command = commandScanner.next();

            switch (command) {
                case "start": {
                    player1 = commandScanner.next();
                    player2 = commandScanner.next();
                    currentPlayer = player1;
                    start();
                    break;
                }
                case "exit": {
                    return;
                }
            }
        }
    }

    private void start() {
        GameState gameState;
        Cell cell;
        clearBoard();
        printBoard();

        while (true) {
            cell = "user".equals(currentPlayer)
                    ? getUserSelectedCell()
                    : getAiSelectedCell();
            makeMove(cell);
            printBoard();
            gameState = detectGameState(cell);

            if (gameState != GameState.RUNNING) break;

            switchMove();
        }

        printGameState(gameState);
    }

    private Cell getUserSelectedCell() {
        Scanner scanner = new Scanner(System.in);
        int cellRow, cellColumn;

        while (true) {
            try {
                System.out.println("Enter the coordinates:");
                cellRow = scanner.nextInt();

                if (cellRow < 1 || cellRow > boardSize) {
                    System.out.println("Coordinates should be from 1 to 3!");
                    scanner.nextLine();
                    continue;
                }

                cellColumn = scanner.nextInt();

                if (cellColumn < 1 || cellColumn > boardSize) {
                    System.out.println("Coordinates should be from 1 to 3!");
                    scanner.nextLine();
                    continue;
                }

                if (board[cellRow - 1][cellColumn - 1] != ' ') {
                    System.out.println("This cell is occupied! Choose another one!");
                    continue;
                }

                return new Cell(cellRow - 1, cellColumn - 1);
            } catch (InputMismatchException exception) {
                System.out.println("You should enter numbers!");
                scanner.nextLine();
            }
        }
    }

    private Cell getAiSelectedCell() {
        switch (currentPlayer) {
            case "hard": {
                System.out.println("Making move level \"hard\"");
                return getHardAiCell();
            }
            case "medium": {
                System.out.println("Making move level \"medium\"");
                return getMediumAiCell();
            }
            default: {
                System.out.println("Making move level \"easy\"");
                return getRandomCell();
            }
        }
    }

    private Cell getRandomCell() {
        List<Cell> emptyCells = new ArrayList<>();

        for (int row = 0; row < boardSize; ++row) {
            for (int column = 0; column < boardSize; ++column) {
                if (board[row][column] == ' ') {
                    emptyCells.add(new Cell(row, column));
                }
            }
        }

        Random random = new Random();
        int cellNumber = random.nextInt(emptyCells.size());
        return emptyCells.get(cellNumber);
    }

    private Cell getMediumAiCell() {
        if (moveHistory.size() == 0) {
            return getRandomCell();
        }

        if (moveHistory.size() > 2) {
            Cell lastCell = moveHistory.get(moveHistory.size() - 2);
            Cell cellForWinningMove = getCellForWinningMove(move, lastCell);

            if (cellForWinningMove != null) return cellForWinningMove;
        }

        Cell lastOpponentCell = moveHistory.get(moveHistory.size() - 1);
        char opponentMove = move == 'X' ? 'O' : 'X';
        Cell cellForWinningMove = getCellForWinningMove(opponentMove, lastOpponentCell);

        if (cellForWinningMove != null) return cellForWinningMove;

        return getRandomCell();
    }

    private Cell getCellForWinningMove(char move, Cell lastCell) {
        int moveCountPerLine = 0;
        Cell emptyCell = null;

        for (int row = 0; row < boardSize; ++row) {
            if (board[row][lastCell.column] == move) ++moveCountPerLine;
            else if (board[row][lastCell.column] == ' ') emptyCell = new Cell(row, lastCell.column);
            if (moveCountPerLine - row > 1) break;
        }

        if (moveCountPerLine == boardSize - 1) return emptyCell;

        moveCountPerLine = 0;

        for (int column = 0; column < boardSize; ++column) {
            if (board[lastCell.row][column] == move) ++moveCountPerLine;
            else if (board[lastCell.row][column] == ' ') emptyCell = new Cell(lastCell.row, column);
            if (moveCountPerLine - column > 1) break;
        }

        if (moveCountPerLine == boardSize - 1) return emptyCell;

        moveCountPerLine = 0;

        if (lastCell.row == lastCell.column) {
            for (int index = 0; index < boardSize; ++index) {
                if (board[index][index] == move) ++moveCountPerLine;
                else if (board[index][index] == ' ') emptyCell = new Cell(index, index);
                if (moveCountPerLine - index > 1) break;
            }
        }

        if (moveCountPerLine == boardSize - 1) return emptyCell;

        moveCountPerLine = 0;

        if (lastCell.row == boardSize - lastCell.column - 1) {
            for (int index = 0; index < boardSize; ++index) {
                if (board[index][boardSize - index - 1] == move) ++moveCountPerLine;
                else if (board[index][boardSize - index - 1] == ' ') emptyCell = new Cell(index, boardSize - index - 1);
                if (moveCountPerLine - index > 1) break;
            }
        }

        if (moveCountPerLine == boardSize - 1) return emptyCell;
        return null;
    }

    private Cell getHardAiCell() {
        int bestScore = Integer.MIN_VALUE;
        Cell cell = new Cell(-1, -1);

        for (int row = 0; row < boardSize; ++row) {
            for (int column = 0; column < boardSize; ++column) {
                if (board[row][column] == ' ') {
                    board[row][column] = move;
                    int moveScore = minimax(new Cell(row, column), false);
                    board[row][column] = ' ';

                    if (moveScore > bestScore) {
                        cell.row = row;
                        cell.column = column;
                        bestScore = moveScore;
                    }
                }
            }
        }

        return cell;
    }

    private int minimax(Cell cell, boolean isMax) {
        int score = getBoardScore(cell);

        if (score != 0 || !isMovesLeft()) return score;

        int bestScore;

        if (isMax) {
            bestScore = Integer.MIN_VALUE;

            for (int row = 0; row < boardSize; ++row) {
                for (int column = 0; column < boardSize; ++column) {
                    if (board[row][column] == ' ') {
                        board[row][column] = move;
                        bestScore = Math.max(bestScore, minimax(new Cell(row, column), false));
                        board[row][column] = ' ';
                    }
                }
            }
        } else {
            bestScore = Integer.MAX_VALUE;
            char opponentMove = move == 'X' ? 'O' : 'X';

            for (int row = 0; row < boardSize; ++row) {
                for (int column = 0; column < boardSize; ++column) {
                    if (board[row][column] == ' ') {
                        board[row][column] = opponentMove;
                        bestScore = Math.min(bestScore, minimax(new Cell(row, column), true));
                        board[row][column] = ' ';
                    }
                }
            }
        }

        return bestScore;
    }

    private int getBoardScore(Cell cell) {
        for (int row = 1; true; ++row) {
            if (board[row][cell.column] != board[row - 1][cell.column]) break;
            if (row == boardSize - 1) {
                return board[row][cell.column] == move
                        ? 1
                        : -1;
            }
        }

        for (int column = 1; true; ++column) {
            if (board[cell.row][column] != board[cell.row][column - 1]) break;
            if (column == boardSize - 1) {
                return board[cell.row][column] == move
                        ? 1
                        : -1;
            }
        }

        if (cell.row == cell.column) {
            for (int index = 1; true; ++index) {
                if (board[index][index] != board[index - 1][index - 1]) break;
                if (index == boardSize - 1) {
                    return board[index][index] == move
                            ? 1
                            : -1;
                }
            }
        }

        if (cell.row == boardSize - cell.column - 1) {
            for (int index = 1; true; ++index) {
                if (board[index][boardSize - index - 1] != board[index - 1][boardSize - index]) break;
                if (index == boardSize - 1) {
                    return board[index][boardSize - index - 1] == move
                            ? 1
                            : -1;
                }
            }
        }

        return 0;
    }

    private boolean isMovesLeft() {
        for (int row = 0; row < boardSize; ++row) {
            for (int column = 0; column < boardSize; ++column) {
                if (board[row][column] == ' ') return true;
            }
        }

        return false;
    }

    private void makeMove(Cell cell) {
        board[cell.row][cell.column] = move;
        moveHistory.add(cell);
    }

    private void switchMove() {
        move = move == 'X' ? 'O' : 'X';
        currentPlayer = currentPlayer.equals(player1) ? player2 : player1;
    }

    private GameState detectGameState(Cell cell) {
        for (int row = 0; true; ++row) {
            if (board[row][cell.column] != move) break;
            if (row == boardSize - 1) {
                return move == 'X'
                        ? GameState.X_WIN
                        : GameState.ZERO_WIN;
            }
        }

        for (int column = 0; true; ++column) {
            if (board[cell.row][column] != move) break;
            if (column == boardSize - 1) {
                return move == 'X'
                        ? GameState.X_WIN
                        : GameState.ZERO_WIN;
            }
        }

        if (cell.row == cell.column) {
            for (int index = 0; true; ++index) {
                if (board[index][index] != move) break;
                if (index == boardSize - 1) {
                    return move == 'X'
                            ? GameState.X_WIN
                            : GameState.ZERO_WIN;
                }
            }
        }

        if (cell.row == boardSize - cell.column - 1) {
            for (int index = 0; true; ++index) {
                if (board[index][boardSize - index - 1] != move) break;
                if (index == boardSize - 1) {
                    return move == 'X'
                            ? GameState.X_WIN
                            : GameState.ZERO_WIN;
                }
            }
        }

        if (moveHistory.size() == boardSize * boardSize) return GameState.DRAW;

        return GameState.RUNNING;
    }

    private void printBoard() {
        System.out.println("-".repeat(boardSize * boardSize));

        for (int row = 0; row < boardSize; ++row) {
            System.out.print("| ");

            for (int column = 0; column < boardSize; ++column) {
                System.out.print(board[row][column] + " ");
            }

            System.out.println("|");
        }

        System.out.println("-".repeat(boardSize * boardSize));
    }

    private void printGameState(GameState gameState) {
        switch (gameState) {
            case DRAW: {
                System.out.println("Draw");
                break;
            }
            case X_WIN: {
                System.out.println("X wins");
                break;
            }
            case ZERO_WIN: {
                System.out.println("O wins");
                break;
            }
            default: {
                System.out.println("Game not finished");
            }
        }
    }

    private void clearBoard() {
        move = 'X';
        moveHistory.clear();

        for (int row = 0; row < boardSize; ++row) {
            for (int column = 0; column < boardSize; ++column) {
                board[row][column] = ' ';
            }
        }
    }
}