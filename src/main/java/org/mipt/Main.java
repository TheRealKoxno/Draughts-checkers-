package org.mipt;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) {


        try {
            List<String> list = readFile("input.txt");
            if (list.isEmpty()) throw new CheckRuleException("Пустой список!");
            List<Position> whitePositions = getPositions(list.get(0).trim(), DRAUGHT.WHITE);

            List<Position> blackPositions = getPositions(list.get(1).trim(), DRAUGHT.BLACK);
            Battle battle = new Battle(whitePositions, blackPositions);

            // удаление первых двух строк с фигурами
            list.remove(0);
            list.remove(0);
            if (list.size() > 0) {
                for (String str : list) {
                    String[] moves = str.trim().split(" ");
                    String moveWhiteStr = moves[0];
                    parse(battle, moveWhiteStr, DRAUGHT.WHITE);
                    if (moves.length == 2) {
                        String moveBlackStr = moves[1];
                        parse(battle, moveBlackStr, DRAUGHT.BLACK);
                    }
                }
            }
            saveResult(battle);

        } catch (IOException | WhiteSquareException | CheckRuleException e) {
            saveResult(e.getMessage());
            System.exit(0);
        }
    }

    /**
     * парсит текую позицю
     *
     * @param battle    игровое поле
     * @param str     позиция
     * @param draught true если верные ход
     * @throws CheckRuleException если ошибка
     * @throws WhiteSquareException если белая клетка
     */
    public static void parse(Battle battle, String str, DRAUGHT draught) throws CheckRuleException, WhiteSquareException {
        if (str.contains("-")) {
            // если должны делать взятие, а мы делаем обычный мув
            if (battle.checkTake(draught)) throw new CheckRuleException("invalid move");
            Position moveWhite = checkPosition(str.split("-")[0], draught);
            Position moveTo = checkPosition(str.split("-")[1], draught);
            if (battle.check(moveWhite)) throw new CheckRuleException("error " + moveWhite);
            battle.move(moveWhite, moveTo);
            //battle.print();
        } else if (str.contains(":")) {
            String[] pos = str.split(":");
            for (int i = 0; i <= pos.length - 2; i++) {
                Position moveWhite = checkPosition(pos[i], draught);
                Position moveTo = checkPosition(pos[i + 1], draught);
                //System.out.println(moveWhite + ":" + moveTo);
                if (battle.check(moveWhite)) throw new CheckRuleException("error, not found " + moveWhite);
                battle.moveWithTake(moveWhite, moveTo);
                // battle.print();
            }
        }
    }


    /**
     * возвращает список позиций шашек на доске
     *
     * @param str     исходная строка с позициями
     * @param draught цвет шашки
     * @return список
     * @throws WhiteSquareException если есть позиция на белой клетке
     */
    public static List<Position> getPositions(String str, DRAUGHT draught) throws WhiteSquareException {
        List<Position> list = new ArrayList<>();
        if (str == null || str.isEmpty()) return list;
        String[] arr = str.split("[\\s+]");
        for (String a : arr) {
            Position position = checkPosition(a, draught);
            list.add(position);
        }
        return list;
    }


    /**
     * проверяет позицию на поле
     *
     * @param pos позиция шашки
     * @return true - если корректна, false - если нет
     * @throws WhiteSquareException - если описание не соотвествует стандарту
     */
    public static Position checkPosition(String pos, DRAUGHT draught) throws WhiteSquareException {
        if (pos.length() != 2) throw new WhiteSquareException();
        Character[] horizontals = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'};
        Character[] verticals = {'1', '2', '3', '4', '5', '6', '7', '8'};
        Character horizontal = pos.toLowerCase().charAt(0);
        if (Arrays.stream(horizontals).noneMatch(horizontal::equals)) throw new WhiteSquareException();
        int hInd = 0;
        for (int i = 0; i < horizontals.length; i++)
            if (horizontals[i].equals(horizontal)) {
                hInd = i + 1;
                break;
            }

        horizontal = pos.charAt(0);
        Character vertical = pos.charAt(1);
        // если не из списка допустимых значений
        if (Arrays.stream(verticals).noneMatch(vertical::equals)) throw new WhiteSquareException();
        int vInd = 0;
        for (int i = 0; i < verticals.length; i++)
            if (verticals[i].equals(vertical)) {
                vInd = i + 1;
                break;
            }

        if (vInd % 2 == 0 && hInd % 2 == 0) return new Position(horizontal, vInd, draught);
        if (vInd % 2 == 1 && hInd % 2 == 1) return new Position(horizontal, vInd, draught);
        throw new WhiteSquareException();
    }


    /**
     * чтение файла
     *
     * @param fileName имя файл
     * @return список строк
     * @throws IOException throws server exception
     */
    public static List<String> readFile(String fileName) throws IOException {
        return Files.lines(Paths.get(fileName)).collect(Collectors.toList());
    }

    /**
     * сохранение content  в output.txt
     *
     */
    public static void saveResult(String content) {
        try {
            Path path = Paths.get("output.txt");
            Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.out.println("Ошибка " + e.getMessage());
        }

    }

    /**
     * сохраняет игровое поле в файл
     *
     * @param battle игровое поле
     */
    public static void saveResult(Battle battle) {
        String fileName = "output.txt";
        String messageToWrite = battle.battleToString();
        try {
            Files.writeString(Paths.get(fileName), messageToWrite, StandardCharsets.UTF_8);
        } catch (IOException ignored) {

        }

    }


    // исключение, если поле белое
    private static class WhiteSquareException extends Exception {
        public WhiteSquareException() {
            super("white cell");
        }

        public WhiteSquareException(String message) {
            super("white cell " + message);
        }
    }

    // если несоблюдение правил
    private static class CheckRuleException extends Exception {
        public CheckRuleException(String message) {
            super(message);
        }
    }

    /**
     * позиция шашки на доске
     */

    private static class Position implements Comparable<Position> {
        final int vertical;
        Character horizontal;
        DRAUGHT draught;
        boolean isDame = false;

        // устанавливает текущую позицию дамки
        public void setDame() {
            isDame = true;
            horizontal = Character.toUpperCase(horizontal);
        }

        // если поле пустое
        public boolean isEmpty() {
            return draught == DRAUGHT.EMPTY;
        }

        // если такая же шашка
        public boolean isSame(Position position) {
            return draught == position.getDraught();
        }


        /**
         * конструктор с параметрам, создает позицию
         *
         * @param horizontal a-h
         * @param vertical   1-8
         * @param draught    цвет или пустая
         */
        public Position(Character horizontal, int vertical, DRAUGHT draught) {
            this.vertical = vertical;
            if (Character.isUpperCase(horizontal)) isDame = true;
            this.horizontal = horizontal;
            this.draught = draught;
        }


        public DRAUGHT getDraught() {
            return draught;
        }

        public void setDraught(DRAUGHT draught) {
            this.draught = draught;
        }

        public int getVertical() {
            return vertical;
        }

        public Character getHorizontal() {
            return horizontal;
        }

        // возврашает индекс в массиве
        public int[] getIndex() {
            Character[] horizontals = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'};
            int hInd = 0;
            for (int i = 0; i < horizontals.length; i++)
                if (horizontals[i].equals(Character.toLowerCase(horizontal))) {
                    hInd = i;
                    break;
                }
            int vInd = 8 - vertical;
            return new int[]{vInd, hInd};
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Position)) return false;
            Position position = (Position) o;
            // игнорируем дамка или нет
            return Character.toLowerCase(horizontal) == Character.toLowerCase(position.horizontal) &&
                    Objects.equals(vertical, position.vertical);
        }

        @Override
        public int hashCode() {
            return Objects.hash(vertical, horizontal);
        }

        @Override
        public String toString() {
            return horizontal + Integer.toString(vertical);
        }

        /**
         * сравнивает две позиции по координатам и цвету, игнорирую дамка или нет
         *
         * @param o позиция для сравнения
         * @return 0 если равны, 1 если больше o, -1 если меньше o
         */
        @Override
        public int compareTo(Position o) {
            if (horizontal.equals(o.getHorizontal()) && vertical == o.getVertical())
                return 0;
            Character lower = horizontal;
            //Character.toLowerCase(horizontal);
            Character lowerO = o.getHorizontal();
            // Character.toLowerCase(o.getHorizontal());
            if (lower.compareTo(lowerO) == 0) {
                return Integer.compare(vertical, o.getVertical());
            }
            return lower.compareTo(lowerO);
        }
    }


    private enum DRAUGHT {
        WHITE, BLACK, EMPTY
    }

    // батл
    private static class Battle {
        List<Position> white;
        List<Position> black;
        Position[][] field = new Position[8][8];

        public Battle(List<Position> white, List<Position> black) {
            this.white = white;
            this.black = black;
            char[] a = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'};
            // изначально все поля пустые
            for (int i = 0; i <= 7; i++) {
                for (int j = 1; j <= 8; j++) {
                    try {
                        String str = String.format("%s%d", a[i], j);
                        Position empty = checkPosition(str, DRAUGHT.EMPTY);
                        int[] index = empty.getIndex();
                        field[index[0]][index[1]] = empty;
                    } catch (WhiteSquareException ignored) {

                    }
                }
            }

            // расставим шашки
            for (Position w : white) {
                int[] p = w.getIndex();
                int c = p[0];
                int r = p[1];
                field[c][r] = w;
            }

            for (Position b : black) {
                int[] p = b.getIndex();
                int c = p[0];
                int r = p[1];
                field[c][r] = b;
            }
        }

        // вывод доски на экран в читаемом виде
        public void print() {
            System.out.println();
            for (int i = 0; i < 8; i++) {
                System.out.print((8 - i) + "|");
                for (int j = 0; j < 8; j++) {
                    Position p = field[i][j];
                    if (p == null) {
                        System.out.print(" |");
                        continue;
                    }
                    if (p.getDraught() == DRAUGHT.WHITE)
                        System.out.print("W|");
                    else if (p.getDraught() == DRAUGHT.BLACK)
                        System.out.print("B|");
                    else System.out.print(" |");
                }
                System.out.println();
            }
            System.out.print(" ");
            for (char c = 'a'; c <= 'h'; c++)
                System.out.print("|" + c);
            System.out.println();
            System.out.println();
        }

        /**
         * возвращает true если шашка есть наигровом поле
         *
         * @param draught шашка
         * @return true или false
         */
        public boolean check(Position draught) {
            int cc = draught.getIndex()[0];
            int rc = draught.getIndex()[1];
            Position position = field[cc][rc];
            return position == null || !position.equals(draught) || !position.isSame(draught);

        }

        /**
         * ход со взятием
         *
         * @param draught позиция шашки, которой делают ход
         * @param to      позиция поля на которое ходит шашка
         * @throws CheckRuleException если нарушено правило
         */
        public void moveWithTake(Position draught, Position to) throws CheckRuleException {
            DRAUGHT from = draught.getDraught();
            int ccTo = to.getIndex()[0];
            int rcTo = to.getIndex()[1];
            Position position = field[ccTo][rcTo];
            // если позиция ошибочная
            if (position == null) throw new CheckRuleException("error");

            int cc = draught.getIndex()[0];
            int rc = draught.getIndex()[1];
            position = field[cc][rc];

            // если позиция ошибочная
            if (position == null) throw new CheckRuleException("error");

            // получим возможную диагональ хода
            List<Position> diagonal = getDiagonal(draught, to);

            // если диагональ меньше 2 - то и ходить то не куда
            if (diagonal.size() < 2) throw new CheckRuleException("error");

            // есть to не принадлежит диагонали - то значит ход не верный
            if (contains(diagonal, to)) throw new CheckRuleException("error");

            // обрежем диагональ до to
            List<Position> newList = diagonalTo(diagonal, to);

            // если диагональ меньше 2 - то и ходить то не куда
            if (newList.size() < 2) throw new CheckRuleException("error");

            if (!newList.get(newList.size()-1).isEmpty()) throw new CheckRuleException("busy cell");

            // если обычна пешка
            if (!draught.isDame || newList.size() == 2) {

                // если следущая тоже такого же цвета - то через неё нельзя ходить
                if (!newList.get(0).isEmpty() && newList.get(0).isSame(draught)) throw new CheckRuleException("error");

                // через клетку тоже нельзя
                if (newList.get(0).isEmpty()) {
                    throw new CheckRuleException("error");
                }

                // если через одну клетку занята - то тоже нельзя
                if (!newList.get(1).isEmpty()) throw new CheckRuleException("error");

                // отметим, что поле откуда ушла шашка - пустое
                Position first = newList.get(0);

                //  уберем шашку
                remove(draught);
                remove(first);
                to.setDraught(draught.getDraught());

                // переставим пешку
                field[ccTo][rcTo] = new Position(to.getHorizontal(), to.getVertical(), from);

                // если стала дамкой
                checkDame(field[ccTo][rcTo]);
            } else {
                Position prev = null;

                for (int i = 0; i <= newList.size() - 2; i++) {
                    Position current = newList.get(i);
                    Position next = newList.get(i + 1);
                    if (current.isEmpty()) {
                        prev = current;

                        // если клетка пуста и конец хода
                        if (current.equals(to)) {
                            field[current.getIndex()[0]][current.getIndex()[1]] = new Position(current.getHorizontal(), current.getVertical(), draught.getDraught());
                            remove(draught);
                            //System.out.println("current to "+to);
                            // если стала дамкой
                            checkDame(field[ccTo][rcTo]);
                            return;
                        }
                    }

                    // если клетка пустая, а следущая тоже пустая и конец хода
                    if (current.isEmpty() && next.isEmpty() && next.equals(to)) {
                        field[next.getIndex()[0]][next.getIndex()[1]] = new Position(next.getHorizontal(), next.getVertical(), draught.getDraught());
                        remove(draught);

                        // если стала дамкой
                        checkDame(field[ccTo][rcTo]);
                        return;
                    }

                    // если клетка не пустая
                    if (!current.isEmpty()
                            && !current.isSame(draught) && next.isEmpty()) {

                        // если предыдушая клетка имее шашку - то ход не возможен
                        if (prev != null && !prev.isEmpty()) throw new CheckRuleException("busy cel");
                        prev = current;
                        remove(current);
                        if (next.equals(to)) {

                            // переставим пешку
                            field[to.getIndex()[0]][to.getIndex()[1]] = new Position(to.getHorizontal(), to.getVertical(), draught.getDraught());

                            // если стала дамкой
                            remove(draught);
                            checkDame(field[ccTo][rcTo]);
                            return;
                        }
                    }
                }

            }

        }

        public boolean checkTake(final DRAUGHT draught) {
            Stream<Position> pos = Arrays.stream(field).flatMap(Arrays::stream);
            List<Position> list = pos.filter(p -> p != null && p.getDraught().equals(draught)).collect(Collectors.toList());
            for (Position position : list) {
                if (checkTake(position)) return true;
            }
            return false;
        }

        // проверка боя по текущей позии
        public boolean checkTake(Position position) {
            int cc = position.getIndex()[0];
            int rc = position.getIndex()[1];
            int cl = cc - 1;
            int rl = rc + 1;
            int clNext = cl - 1;
            int rlNext = rl + 1;

            if (checkTake(position, cl, rl, clNext, rlNext)) return true;

            cl = cc + 1;
            rl = rc + 1;
            clNext = cl + 1;
            rlNext = rl + 1;

            if (checkTake(position, cl, rl, clNext, rlNext)) return true;


            cl = cc - 1;
            rl = rc - 1;
            clNext = cl - 1;
            rlNext = rl - 1;

            if (checkTake(position, cl, rl, clNext, rlNext)) return true;

            return checkTake(position, cl, rl, clNext, rlNext);

        }

        /**
         * проверка позици position и клеток по координатам c,r и cN, rN (следующая по диагонали и снова следующая)
         *
         * @param position позиция
         * @param c        координаты
         * @return true есть ходы в которые нужно быть чужуюклетку
         */
        private boolean checkTake(Position position, int c, int r, int cN, int rN) {
            try {

                // получим левую и левую выше(ниже)
                Position left = field[c][r];
                Position nextLeft = field[cN][rN];
                List<Position> diag = getDiagonal(left, nextLeft);
                if (!position.isDame || (diag.size() == 2)) {

                    // если нужен бой - то дальше можно не проверять
                    if (!left.isEmpty() && !left.isSame(position) && nextLeft.isEmpty()) {
                        return true;
                    }
                } else {
                    Position prev = null;
                    for (int i = 0; i < diag.size() - 2; i++) {
                        Position current = diag.get(i);
                        Position next = diag.get(i + 1);
                        if (prev == null) {
                            if (!current.isEmpty() && !current.isSame(position) && next.isEmpty()) return true;
                        } else if (prev.isEmpty() && !current.isEmpty() && !current.isSame(position) && next.isEmpty())
                            return true;
                        prev = current;
                    }
                }
            } catch (Exception ignored) {

            }
            return false;
        }

        /**
         * удаляет шашку с поля, заменяя пустой клеткой
         *
         * @param current позици шашки
         */
        private void remove(Position current) {
            int ccTo = current.getIndex()[0];
            int rcTo = current.getIndex()[1];

            field[ccTo][rcTo] = new Position(current.getHorizontal(), current.getVertical(), DRAUGHT.EMPTY);
        }

        /**
         * проверка стала ли шашка дамкой
         *
         */
        private void checkDame(Position draught) {
            int ccTo = draught.getIndex()[0];
            int rcTo = draught.getIndex()[1];
            if (draught.getDraught() == DRAUGHT.WHITE && ccTo == 0) field[ccTo][rcTo].setDame();
            if (draught.getDraught() == DRAUGHT.BLACK && ccTo == 7) field[ccTo][rcTo].setDame();
        }

        // ход шашки без взятия
        public void move(Position draught, Position to) throws CheckRuleException, WhiteSquareException {
            int ccTo = to.getIndex()[0];
            int rcTo = to.getIndex()[1];
            Position position = field[ccTo][rcTo];
            if (position == null) throw new WhiteSquareException(to.toString());

            int cc = draught.getIndex()[0];
            int rc = draught.getIndex()[1];
            position = field[cc][rc];
            if (position == null) throw new WhiteSquareException(draught.toString());
            List<Position> diagonal = getDiagonal(draught, to);
            // если диагональ пустая - то ходить некуда
            if (diagonal.isEmpty()) throw new CheckRuleException("error нет хода");
            if (contains(diagonal, to)) throw new CheckRuleException("error ошибочный ход");
            diagonal = diagonalTo(diagonal, to);
            if (diagonal.isEmpty()) throw new CheckRuleException("error нет хода");
            // если ходит не дамка то она может ходить только на следующую клетку
            if (!draught.isDame || (diagonal.size() == 2)) {
                Position first = diagonal.get(0);
                //if (!first.equals(to)) throw new CheckRuleException("error 1");
                if (first.equals(to) && !first.isEmpty()) throw new CheckRuleException("busy cell ");
            } else {
                boolean flag = false;

                for (Position pos : diagonal) {
                    if (pos.equals(to) && !pos.isEmpty()) throw new CheckRuleException("busy cell ");
                    if (pos.equals(to)) flag = true;
                }

                // если неверный ход
                if (!flag) {
                    throw new CheckRuleException("error");
                }

            }

            // отмечаем, что поле предыдущее поле шашки пустое
            remove(draught);
            field[ccTo][rcTo] = new Position(to.getHorizontal(), to.getVertical(), draught.getDraught());
            checkDame(field[ccTo][rcTo]);
        }

        // проверяем есть ли шашка position в списке list
        boolean contains(List<Position> list, final Position position) {
            return (list.stream().noneMatch(p -> p.toString().equalsIgnoreCase(position.toString())));
        }

        // получаем все шашки на диагонали мува
        List<Position> getDiagonal(Position draught, Position to) {
            List<Position> positions = new ArrayList<>();
            int cc = draught.getIndex()[0];
            int rc = draught.getIndex()[1];
            int ccTo = to.getIndex()[0];
            int rcTo = to.getIndex()[1];
            if (cc > ccTo && rc > rcTo) {
                while (cc > 0 && rc > 0) {
                    cc--;
                    rc--;
                    if (field[cc][rc] != null) {
                        positions.add(field[cc][rc]);
                    }
                }
                return positions;
            }
            if (cc > ccTo && rc < rcTo) {
                while (cc > 0 && rc < 7) {
                    cc--;
                    rc++;
                    if (field[cc][rc] != null) {
                        positions.add(field[cc][rc]);
                    }
                }
                return positions;
            }
            if (cc < ccTo && rc > rcTo) {
                while (cc < 7 && rc > 0) {
                    cc++;
                    rc--;
                    if (field[cc][rc] != null) {
                        positions.add(field[cc][rc]);
                    }
                }
                return positions;
            }
            if (cc < ccTo && rc < rcTo) {
                while (cc < 7 && rc < 7) {
                    cc++;
                    rc++;
                    if (field[cc][rc] != null) {
                        positions.add(field[cc][rc]);
                    }

                }
                return positions;
            }
            return positions;

        }

        // обрезаем диагональ до позиции to
        public List<Position> diagonalTo(List<Position> list, Position to) {
            List<Position> newList = new ArrayList<>();
            if (list.isEmpty()) return newList;
            newList.add(list.get(0));
            for (int i = 1; i < list.size(); i++) {
                newList.add(list.get(i));
                if (list.get(i).equals(to)) break;
            }
            return newList;
        }

        // преобразование игрового поля в строку
        public String battleToString() {
            Stream<Position> pos = Arrays.stream(field).flatMap(Arrays::stream);
            String white = pos.filter(p -> p != null && p.getDraught() == DRAUGHT.WHITE).sorted().map(Position::toString).collect(Collectors.joining(" "));
            pos = Arrays.stream(field).flatMap(Arrays::stream);
            String black = pos.filter(p -> p != null && p.getDraught() == DRAUGHT.BLACK).sorted().map(Position::toString).collect(Collectors.joining(" "));
            return white + "\r\n" + black;
        }

    }
}

