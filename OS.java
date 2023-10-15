import java.io.*;
import java.util.*;
import java.lang.*;

public class OS {
    private static char[][] M = new char[300][4];
    private char[] buffer = new char[40];
    private static Integer[] flag = new Integer[30];
    private static String line;

    // registers
    private static char[] IR = new char[4];
    private static char[] R = new char[4];
    private static int C = 0;
    private static int IC = 0;
    private static int PTR = 0;
    private static int VA = 0;
    private static int RA = 0;
    private static int sRA = 0;
    private static int kio = -1;

    // interupts
    private static int SI = 0;
    private static int PI = 0;
    private static int TI = 0;
    private static int iSBT = 0;

    // interupt counters
    private static int TTC = 0;
    private static int LLC = 0;

    private static boolean endProgram = false;

    private String inputFile;
    private String outputFile;
    private FileReader input;
    private BufferedReader fread;
    private FileWriter output;
    private BufferedWriter fwrite;

    PCB pcb = new PCB();
    Random rd = new Random();

    public OS(String ifile, String ofile) {
        this.inputFile = ifile;
        this.outputFile = ofile;

        try {
            this.input = new FileReader(inputFile);
            this.fread = new BufferedReader(input);
            this.output = new FileWriter(outputFile);
            this.fwrite = new BufferedWriter(output);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void dispMemo() {
        for (int i = 0; i < 300; i++) {
            System.out.println("memory[" + i + "] " + new String(M[i]));
        }
        System.out.print("\n");
    }

    public void allocate() {
        int pos, check = 0, len, level = 0;
        char[] str = new char[2];

        while (check != 1) {
            kio++; // 1
            pos = Math.abs((rd.nextInt() % 29) * 10); // 170
            while (flag[pos / 10] != 0) {
                pos = Math.abs((rd.nextInt() % 29) * 10);
            }
            flag[pos / 10] = 1; // 17 = 1
            str = Integer.toString(pos).toCharArray(); // 170
            System.out.println(pos);
            if (pos / 100 == 0) {
                M[PTR + kio][2] = '0'; // 28 -> **0*
                M[PTR + kio][3] = str[0]; // 20 -> **09
            } else {
                M[PTR + kio][2] = str[0]; //
                M[PTR + kio][3] = str[1];
            }
            try {
                line = fread.readLine(); // H
                buffer = line.toCharArray(); // H
                level++; // 3
                int k = 0;
                for (int i = 0; i < (line.length() / 4) + 1; i++) { // 1 times
                    for (int j = 0; j < 4; j++) { // 4 times
                        if (buffer[k] != 'H') {
                            System.out.println(buffer[k]);
                            M[pos + i][j] = buffer[k]; // 20[0] -> H
                        } else {
                            check = 1;
                            M[pos + (i)][0] = 'H'; // 125 -> H
                            M[pos + (i)][1] = '0'; // 125 -> H0
                            M[pos + (i)][2] = '0'; // 125 -> H00
                            M[pos + (i)][3] = '0'; // 125 -> H000
                            break;
                        }
                        k++; // 40
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void MOS() {
        try {
            if (PI == 1) {
                System.out.println("4: Opcode Error. Program terminated abnormally.");
                fwrite.write("4: Opcode Error. Program terminated abnormally.\n");
                endProgram();
                return;
            } else if (PI == 2) {
                System.out.println("5: Oprand Error. Program terminated abnormally.");
                fwrite.write("5: Oprand Error. Program terminated abnormally.\n");
                endProgram();
                return;
            } else if (PI == 3) {
                System.out.println("6: Invalid Page fault. Program terminated abnormally.");
                fwrite.write("6: Invalid Page fault. Program terminated abnormally.\n");
                endProgram();
                return;
            }

            if (TI == 2) {
                System.out.println("3: Time limit exceeded. Program terminated abnormally.");
                fwrite.write("3: Time limit exceeded. Program terminated abnormally.\n");
                endProgram();
                return;
            }

            if (SI == 3) {
                endProgram();
            } else if (SI == 1) {
                if (TI == 0) {
                    read();
                } else if (TI == 2) {
                    System.out.println("3: Time limit exceeded. Program terminated abnormally.");
                    fwrite.write("3: Time limit exceeded. Program terminated abnormally.\n");
                    endProgram();
                    return;
                } else if (TI == 1) {
                    System.out.println("2: Line limit exceeded. Program terminated abnormally.");
                    fwrite.write("2: Line limit exceeded. Program terminated abnormally.\n");
                    endProgram();
                    return;
                }
            } else if (SI == 2) {
                if (TI == 0) {
                    System.out.println("Going to call write");
                    write();
                } else if (TI == 2) {
                    write();
                    System.out.println("3: Time limit exceeded. Program terminated abnormally.");
                    fwrite.write("3: Time limit exceeded. Program terminated abnormally.\n");
                    endProgram();
                    return;
                } else if (TI == 1) {
                    System.out.println("2: Line limit exceeded. Program terminated abnormally.");
                    fwrite.write("2: Line limit exceeded. Program terminated abnormally.\n");
                    endProgram();
                    return;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void endProgram() {
        dispMemo();
        endProgram = true;
        try {
            fwrite.write("SI: " + SI + " PI: " + PI + " TI: " + TI + " TTC: " + TTC + " LLC: " + LLC);
            System.out.println("SI: " + SI + " PI: " + PI + " TI: " + TI + " TTC: " + TTC + " LLC: " + LLC);
            fwrite.newLine();
            fwrite.newLine();
            // fwrite.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // System.exit(0);
    }

    public void read() {
        int no;
        try {
            line = fread.readLine(); // *
            buffer = line.toCharArray();
            if (line.contains("$END")) {
                System.out.println("1: Out Of Data. Program terminated abnormally.");
                fwrite.write("1: Out Of Data. Program terminated abnormally.\n");
                endProgram();
                return;
            }
            no = sRA;
            int k = 0;

            for (int i = 0; k < line.length(); i++) { // i = 1
                for (int j = 0; j < 4 && k < line.length(); j++) { // j = 1
                    M[no + i][j] = buffer[k]; // 210 -> *
                    k++; // 1
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void write() {
        System.out.println("Inside Write");
        int no;
        try {
            // no = Integer.parseInt(String.valueOf(M[RA][2]) + String.valueOf(M[RA][3]));
            // no = no * 10; // 120
            no = sRA;
            int k = 0;

            while (true) {
                for (int i = 0; i < 4; i++) {
                    if (M[no][i] == '\0') {
                        break;
                    }
                    // buffer[k] = M[no][i];
                    fwrite.write(M[no][i]);
                    k++;
                }
                if (M[no][0] == '\0') {
                    break;
                }
                no++;

            }
            System.out.println(buffer);
            // fwrite.newLine();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addMap() {
        int add, pos;
        char[] str = new char[2];

        RA = PTR + (VA / 10);

        if (M[RA][3] == '*') {
            System.out.println("Page fault occur");

            if (IR[0] == 'P' || IR[0] == 'L' || IR[0] == 'C') {
                PI = 3;
                MOS();
                return;
            }
            pos = Math.abs((rd.nextInt() % 29) * 10); // 160
            while (flag[pos / 10] != 0) {
                pos = Math.abs((rd.nextInt() % 29) * 10);
            }
            flag[pos / 10] = 1; // 16 = 1
            str = Integer.toString(pos).toCharArray(); // 160
            if (pos / 100 == 0) {
                M[RA][2] = '0';
                M[RA][3] = str[0];
            } else {
                M[RA][2] = str[0]; // 12 -> **1*
                M[RA][3] = str[1]; // 12 -> **16
            }
        }
        if (RA > PTR + 10) {
            PI = 2;
            MOS();
        }
        int PTE = PTR + (VA / 10); // 263
        int mpte = Integer.parseInt(String.valueOf(M[PTE][2]) + String.valueOf(M[PTE][3])); // 16
        sRA = mpte * 10 + (VA % 10);
    }

    public void examine() {
        char ch = IR[0]; // S
        switch (ch) {
            case 'G':
                System.out.println("SI:" + SI + " TI:" + TI + " PI:" + PI + "TTC: " + TTC + " LLC: " + LLC);
                if (IR[1] != 'D') {
                    PI = 1;
                    MOS();
                } else {
                    TTC = TTC + 2;
                    if (TTC <= pcb.ttl) { // ttc = 2
                        SI = 1;
                        MOS();
                    } else {
                        TI = 2;
                        MOS();
                    }
                }
                System.out.println("SI:" + SI + " TI:" + TI + " PI:" + PI + "TTC: " + TTC + " LLC: " + LLC);
                SI = PI = TI = 0;
                break;
            case 'P':
                SI = 2;
                System.out.println("SI:" + SI + " TI:" + TI + " PI:" + PI + "TTC: " + TTC + " LLC: " + LLC);
                if (IR[1] != 'D') {
                    PI = 1;
                    MOS();
                } else {
                    System.out.println(pcb.tll);
                    System.out.println(LLC);
                    LLC = LLC + 1; // 1
                    System.out.println(LLC);
                    TTC = TTC + 1; // 3
                    if (LLC > pcb.tll) {
                        System.out.println("Inside LLC < tll");
                        TI = 1;
                        MOS();
                    }
                    if (TTC > pcb.ttl) {
                        System.out.println("Inside TTC < ttl");
                        TI = 2;
                        MOS();
                    } else {
                        SI = 2;
                        System.out.println("Going to call MOS");
                        MOS();
                        try {
                            fwrite.newLine();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                System.out.println("SI:" + SI + " TI:" + TI + " PI:" + PI + "TTC: " + TTC + " LLC: " + LLC);
                SI = PI = TI = 0;
                break;
            case 'L':
                System.out.println("SI:" + SI + " TI:" + TI + " PI:" + PI + "TTC: " + TTC + " LLC: " + LLC);
                if (IR[1] != 'R') {
                    PI = 1;
                    MOS();
                } else {
                    TTC++;
                    if (TTC <= pcb.ttl) {
                        for (int i = 0; i < 4; i++) {
                            // get 2 and 3rd index of RA
                            // int no = Integer.parseInt(String.valueOf(M[RA][2]) +
                            // String.valueOf(M[RA][3]));
                            // no = no * 10;
                            R[i] = M[sRA][i];
                        }
                    } else {
                        TI = 2;
                        MOS();
                    }
                }
                System.out.println("SI:" + SI + " TI:" + TI + " PI:" + PI + "TTC: " + TTC + " LLC: " + LLC);
                break;
            case 'S':
                System.out.println("SI:" + SI + " TI:" + TI + " PI:" + PI + "TTC: " + TTC + " LLC: " + LLC);
                if (IR[1] != 'R') {
                    PI = 1;
                    MOS();
                } else {
                    TTC++;
                    if (TTC <= pcb.ttl) {
                        System.out.println("Inside TTC check");
                        for (int i = 0; i < 4; i++) {
                            M[sRA][i] = R[i];
                        }
                    } else {
                        TI = 2;
                        MOS();
                    }
                }
                System.out.println("SI:" + SI + " TI:" + TI + " PI:" + PI + "TTC: " + TTC + " LLC: " + LLC);
                break;
            case 'C':
                System.out.println("SI:" + SI + " TI:" + TI + " PI:" + PI + "TTC: " + TTC + " LLC: " + LLC);
                if (IR[1] != 'R') {
                    PI = 1;
                    MOS();
                } else {
                    System.out.println("bauidaudbakdbuakdbakdbakdbkdkb");
                    System.out.println(String.valueOf(M[sRA][0]) + String.valueOf(M[sRA][1]) + String.valueOf(M[sRA][2])
                            + String.valueOf(M[sRA][3]) + "  "
                            + String.valueOf(R[0]) + String.valueOf(R[1]) + String.valueOf(R[2])
                            + String.valueOf(R[3]));
                    TTC++;
                    if (TTC <= pcb.ttl) {
                        int res = 1;
                        for (int i = 0; i < 4; i++)
                            if (M[sRA][i] != R[i])
                                res = 0;
                        C = res;
                        // res = 0;
                    } else {
                        TI = 2;
                        MOS();
                    }
                }
                System.out.println("SI:" + SI + " TI:" + TI + " PI:" + PI + "TTC: " + TTC + " LLC: " + LLC);
                break;
            case 'B':
                System.out.println("SI:" + SI + " TI:" + TI + " PI:" + PI + "TTC: " + TTC + " LLC: " + LLC);
                if (IR[1] != 'T') {
                    PI = 1;
                    MOS();
                } else {
                    TTC++;
                    if (TTC <= pcb.ttl) {
                        iSBT = 1;
                        if (C == 1)
                            IC = Integer.parseInt(String.valueOf(IR[2]) + String.valueOf(IR[3]));
                        System.out.println("Inside BTTTTTTTT.....");
                        System.out.println(IC);
                        C = 0;
                    } else {
                        TI = 2;
                        MOS();
                    }
                }
                System.out.println("SI:" + SI + " TI:" + TI + " PI:" + PI + "TTC: " + TTC + " LLC: " + LLC);
                break;
            case 'H':
                SI = 3;
                MOS();
                break;
            default:
                PI = 1;
                MOS();
                break;
        }
    }

    public void executeProgram() {
        int no;
        char[] a = new char[3];
        for (int i = 0; i <= kio; i++) { // kio = 2
            a[0] = M[PTR + i][2]; //
            a[1] = M[PTR + i][3]; // 04
            a[2] = '\0';

            no = Integer.parseInt(String.valueOf(a).trim()); // 4
            for (IC = 0; IC < 10;) {
                for (int k = 0; k < 4; k++) {
                    IR[k] = M[no * 10 + IC][k];
                } // IR = GD30
                IC++;
                if (Character.isDigit(IR[2]) && Character.isDigit(IR[3])) {
                    if (IR[0] != '\0') {
                        System.out.println("IR: " + String.valueOf(IR).trim());

                        VA = Integer.parseInt(String.valueOf(IR).substring(2, 4)); // 20
                        System.out.println("VA: " + VA);
                        addMap();
                        if (endProgram) {
                            break;
                        }
                        examine();
                        if (endProgram) {
                            break;
                        }
                    }
                    if (endProgram) {
                        break;
                    }
                } else {
                    PI = 2;
                    MOS();
                    if (endProgram) {
                        break;
                    }
                }
            }
            if (endProgram) {
                break;
            }
        }
    }

    public void startExecution() {
        IC = 0;
        executeProgram();
    }

    public void initialize() {
        endProgram = false;
        int i, j;
        // page address
        PTR = Math.abs((rd.nextInt() % 29) * 10); // 50
        for (i = 0; i < 30; i++) {
            flag[i] = 0;
        }
        System.out.println("PTR: " + PTR); // 50
        // mark PTR block as occupied
        flag[PTR / 10] = 1; // flag[5] = 1

        // set all memory as default
        for (i = 0; i < 300; i++) {
            for (j = 0; j < 4; j++) {
                M[i][j] = '\0';
            }
        }

        // Page table register
        for (i = PTR; i < PTR + 10; i++) {
            for (j = 0; j < 4; j++) {
                M[i][j] = '*';
            }
        }
        for (i = 0; i < 4; i++) {
            IR[i] = '\0';
            R[i] = '\0';
        }
        C = IC = VA = RA = SI = PI = TI = TTC = LLC = sRA = 0;
        kio = -1;
    }

    public void load() {
        try {
            while ((line = fread.readLine()) != null) {
                System.out.println(line);
                if (line.contains("$AMJ")) {
                    System.out.println("Found $AMJ");
                    pcb.jobid = line.substring(4, 8); // 0202
                    pcb.ttl = Integer.parseInt(line.substring(8, 12)); // 0017
                    pcb.tll = Integer.parseInt(line.substring(12, 16)); // 0005

                    initialize();
                    allocate();
                } else if (line.contains("$DTA")) {
                    startExecution();
                }
            }
            fwrite.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}