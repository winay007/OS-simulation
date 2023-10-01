import java.io.*;
import java.util.*;

public class OS {

    // Core data structures for Operating System
    private char[][] memory = new char[300][4];
    private char[] buffer = new char[40];
    private char[] R = new char[4];
    private char[] IR = new char[4];
    private Integer[] flag = new Integer[30];
    private int IC;
    private int T;
    private int SI;
    private int PI;
    private int TI;
    private int PTR;
    private int RA;
    private int VA;
    private int sRA;

    private int kio = -1;
    private int TTL;
    private int LLC;
    private int TTC;

    private boolean endOfProgram = false;

    PCB pcb = new PCB();
    Random rnd = new Random();

    // Non core data structures
    private int memory_used;
    // private int data_card_skip=0;
    // private String input_file;
    // private String output_file;
    private FileReader input;
    private BufferedReader fread;
    private FileWriter output;

    public OS(String file, String output) {
        // this.input_file=file;
        this.SI = 0;
        try {
            this.input = new FileReader(file);
            this.fread = new BufferedReader(input);
            this.output = new FileWriter(output);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void init() {
        memory_used = 0;
        memory = null;
        endOfProgram = false;
        memory = new char[300][4];
        PTR = rnd.nextInt(30);

        // intialize all blocks as unused
        for (int i = 0; i < 30; i++)
            flag[i] = 0;

        // set page table block as used
        System.out.println("PTR: " + PTR);
        flag[PTR] = 1;

        for (int i = 0; i < 300; i++) {
            for (int j = 0; j < 4; j++) {
                memory[i][j] = '\0';
            }
        }

        // intialize all page table entries by default value
        for (int i = PTR; i < PTR + 10; i++)
            for (int j = 0; j < 4; j++)
                memory[i][j] = '*';

        for (int i = 0; i < 4; i++) {
            IR[i] = '\0';
            R[i] = '\0';
        }
        // data_card_skip=0;
        IC = VA = RA = SI = PI = TI = TTC = LLC = sRA = 0;
        kio = -1;
        IC = 0;
    }

    public void allocate() {
        int pos = 0, check = 0, len, level = 0;
        char str[] = new char[2];

        while (check != 1) {
            kio++;
            pos = Math.abs((rnd.nextInt() % 29) * 10);
            while (flag[pos / 10] != 0)
                pos = Math.abs((rnd.nextInt() % 29) * 10);
            flag[pos / 10] = 1;
            str = Integer.toString(pos).toCharArray();
            System.out.println("Assigned Frame Number: " + str[0] + str[1]);

            // entry in page table
            if (pos < 100) {
                memory[PTR + kio][2] = '0';
                memory[PTR + kio][3] = str[0];
            } else {
                memory[PTR + kio][2] = str[0];
                memory[PTR + kio][3] = str[1];
            }

            String line;
            try {
                line = fread.readLine();
                buffer = line.toCharArray();
                for (int i = 0; i < line.length();) {
                    if (buffer[i] != 'H') {
                        memory[pos][i % 4] = buffer[i];
                        i++;
                        if (i % 4 == 0)
                            pos++;
                    } else {
                        memory[pos][0] = 'H';
                        memory[pos][1] = '0';
                        memory[pos][2] = '0';
                        memory[pos][3] = '0';
                        break;
                    }

                }
                check = 1;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void LOAD() {
        // int flag=0;
        String line;
        try {
            while ((line = fread.readLine()) != null) {
                buffer = line.toCharArray();
                if (buffer[0] == '$' && buffer[1] == 'A' && buffer[2] == 'M' && buffer[3] == 'J') {
                    System.out.println("Program card detected");
                    pcb.jobid = line.substring(4, 8);
                    pcb.ttl = Integer.parseInt(line.substring(8, 12));
                    pcb.tll = Integer.parseInt(line.substring(12, 16));
                    init();
                    allocate();
                    continue;
                } else if (buffer[0] == '$' && buffer[1] == 'D' && buffer[2] == 'T' && buffer[3] == 'A') {
                    System.out.println("Data card detected");
                    startExecution();
                    continue;
                } else if (buffer[0] == '$' && buffer[1] == 'E' && buffer[2] == 'N' && buffer[3] == 'D') {
                    System.out.println("END card detected");
                    output.write("\n\n");
                    print_memory();
                    continue;
                }
            }
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // System.out.println(memory_used);
        // print_memory();
    }

    public void addMap() {
        int add, pos;
        char[] str = new char[2];

        RA = PTR + (VA / 10);

        // page fault
        if (memory[RA][3] == '*') {
            System.out.println("Page fault occur");

            // invalid page fault
            if (IR[0] == 'P' || IR[0] == 'L' || IR[0] == 'C') {
                PI = 3;
                mos();
                return;
            }

            // valid page fault
            pos = Math.abs((rnd.nextInt() % 29) * 10); // 160
            while (flag[pos / 10] != 0) {
                pos = Math.abs((rnd.nextInt() % 29) * 10);
            }
            flag[pos / 10] = 1; // 16 = 1
            str = Integer.toString(pos).toCharArray(); // 160
            if (pos / 100 == 0) {
                memory[RA][2] = '0';
                memory[RA][3] = str[0];
            } else {
                memory[RA][2] = str[0];
                memory[RA][3] = str[1];
            }

        }

        // invalid operand
        if (RA > PTR + 10) {
            PI = 2;
            mos();
        }

        int PTE = PTR + (VA / 10); // 263
        int mpte = Integer.parseInt(String.valueOf(memory[PTE][2]) + String.valueOf(memory[PTE][3]));
        sRA = mpte * 10 + (VA % 10);
    }

    private void startExecution() {
        int no;
        char[] a = new char[3];
        for (int i = 0; i <= kio; i++) { // kio = 2

            // frame number frame page table (PTR)
            a[0] = memory[PTR + i][2]; //
            a[1] = memory[PTR + i][3]; // 04
            a[2] = '\0';

            no = Integer.parseInt(String.valueOf(a).trim());
            for (int j = 0; j < 10; j++) {

                // putting current instruction into IR register
                for (int k = 0; k < 4; k++) {
                    IR[k] = memory[no * 10 + j][k];
                }

                // check if valid instruction
                if (Character.isDigit(IR[2]) && Character.isDigit(IR[3])) {
                    if (IR[0] != '\0') {
                        System.out.println("IR: " + String.valueOf(IR).trim());

                        VA = Integer.parseInt(String.valueOf(IR).substring(2, 4)); // 20
                        System.out.println("VA: " + VA);
                        addMap();
                        if (endOfProgram) {
                            break;
                        }
                        execute();
                        if (endOfProgram) {
                            break;
                        }
                    }
                    if (endOfProgram) {
                        break;
                    }
                }
                // throw operand error
                else {
                    PI = 2;
                    mos();
                    if (endOfProgram) {
                        break;
                    }
                }
            }
            if (endOfProgram) {
                break;
            }
        }
    }

    private void execute() {
        char ch = IR[0];

        switch (ch) {
            case 'L': {
                if (IR[1] != 'R') {
                    PI = 1;
                    mos();
                } else {
                    TTC++;
                    if (TTC <= pcb.ttl) {
                        String line = new String(IR);
                        int num = Integer.parseInt(line.substring(2));
                        R[0] = memory[num][0];
                        R[1] = memory[num][1];
                        R[2] = memory[num][2];
                        R[3] = memory[num][3];
                    } else {
                        TI = 2;
                        mos();
                    }
                }
                break;
            }

            case 'S': {
                if (IR[1] != 'R') {
                    PI = 1;
                    mos();
                } else {
                    TTC++;
                    if (TTC <= pcb.ttl) {
                        String line = new String(IR);
                        int num = Integer.parseInt(line.substring(2));
                        memory[num][0] = R[0];
                        memory[num][1] = R[1];
                        memory[num][2] = R[2];
                        memory[num][3] = R[3];
                    } else {
                        TI = 2;
                        mos();
                    }
                }
                break;
            }

            case 'C': {
                if (IR[1] != 'R') {
                    PI = 1;
                    mos();
                } else {
                    TTC++;
                    if (TTC <= pcb.ttl) {
                        String line = new String(IR);
                        int num = Integer.parseInt(line.substring(2));
                        if (memory[num][0] == R[0] && memory[num][1] == R[1] && memory[num][2] == R[2]
                                && memory[num][3] == R[3]) {
                            T = 1;
                        } else {
                            T = 0;
                        }
                    } else {
                        TI = 2;
                        mos();
                    }
                }
                break;
            }

            case 'B': {
                if (IR[1] != 'T') {
                    PI = 1;
                    mos();
                } else {
                    TTC++;
                    if (TTC <= pcb.ttl) {
                        if (T == 1) {
                            String line = new String(IR);
                            int num = Integer.parseInt(line.substring(2));
                            IC = num;
                            T = 0;
                        }
                    } else {
                        TI = 2;
                        mos();
                    }
                }
                break;
            }

            case 'G': {
                if (IR[1] != 'D') {
                    PI = 1;
                    mos();
                } else {
                    TTC++;
                    if (TTC <= pcb.ttl) {
                        SI = 1;
                        mos();
                    } else {
                        TI = 2;
                        mos();
                    }
                }
                break;
            }

            case 'P': {
                if (IR[1] != 'D') {
                    PI = 1;
                    mos();
                } else {
                    TTC++;
                    if (TTC <= pcb.ttl) {
                        SI = 2;
                        mos();
                    } else {
                        TI = 2;
                        mos();
                    }
                }
                break;
            }

            case 'H': {
                TTC++;
                if (TTC <= pcb.ttl) {
                    SI = 3;
                    mos();
                } else {
                    TI = 2;
                    mos();
                }
                break;
            }

            default:
                PI = 1;
                mos();
                break;
        }
    }

    private void mos() {
        try {

            // error interrupt
            if (PI == 1) {
                System.out.println("4: Opcode Error. Program terminated abnormally.");
                output.write("4: Opcode Error. Program terminated abnormally.\n");
                endProgram();
                return;
            } else if (PI == 2) {
                System.out.println("5: Oprand Error. Program terminated abnormally.");
                output.write("5: Oprand Error. Program terminated abnormally.\n");
                endProgram();
                return;
            } else if (PI == 3) {
                System.out.println("6: Invalid Page fault. Program terminated abnormally.");
                output.write("6: Invalid Page fault. Program terminated abnormally.\n");
                endProgram();
                return;
            }

            if (TI == 2) {
                System.out.println("3: Time limit exceeded. Program terminated abnormally.");
                output.write("3: Time limit exceeded. Program terminated abnormally.\n");
                endProgram();
                return;
            }

            // halt interrupt
            if (SI == 3) {
                endProgram();
            }

            // read interrupt
            else if (SI == 1) {
                if (TI == 0) {
                    Read();
                } else if (TI == 2) {
                    System.out.println("3: Time limit exceeded. Program terminated abnormally.");
                    output.write("3: Time limit exceeded. Program terminated abnormally.\n");
                    endProgram();
                    return;
                } else if (TI == 1) {
                    System.out.println("2: Line limit exceeded. Program terminated abnormally.");
                    output.write("2: Line limit exceeded. Program terminated abnormally.\n");
                    endProgram();
                    return;
                }
            }
            // write interrupt
            else if (SI == 2) {
                if (TI == 0) {
                    System.out.println("Going to call write");
                    Write();
                } else if (TI == 2) {
                    Write();
                    System.out.println("3: Time limit exceeded. Program terminated abnormally.");
                    output.write("3: Time limit exceeded. Program terminated abnormally.\n");
                    endProgram();
                    return;
                } else if (TI == 1) {
                    System.out.println("2: Line limit exceeded. Program terminated abnormally.");
                    output.write("2: Line limit exceeded. Program terminated abnormally.\n");
                    endProgram();
                    return;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void Write() {
        IR[3] = '0';
        // String line = new String(IR);
        int num = sRA;
        String t, total = "";
        for (int i = 0; i < 10; i++) {
            t = new String(memory[num + i]);
            total = total.concat(t);
        }
        System.out.println(total + " In write");
        try {
            output.write(total);
            output.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void Read() {
        IR[3] = '0';
        String line = new String(IR);

        int num;
        try {
            line = fread.readLine();
            buffer = line.toCharArray();
            if (line.contains("$END")) {
                System.out.println("1: Out Of Data. Program terminated abnormally.");
                output.write("1: Out Of Data. Program terminated abnormally.\n");
                endProgram();
                return;
            }
            num = sRA;
            for (int i = 0; i < line.length();) {
                // System.out.println(buffer[i]);
                memory[num][(i % 4)] = buffer[i];
                i++;
                if (i % 4 == 0)
                    num++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void endProgram() {
        endOfProgram = true;
        try {
            output.write("SI: " + SI + " PI: " + PI + " TI: " + TI + " TTC: " + TTC + " LLC: " + LLC);
            System.out.println("SI: " + SI + " PI: " + PI + " TI: " + TI + " TTC: " + TTC + " LLC: " + LLC);
            output.write("\n\n");
            print_memory();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void print_memory() {
        for (int i = 0; i < 100; i++) {
            System.out.println("memory[" + i + "] " + new String(memory[i]));
        }
        System.out.println();
    }
}
