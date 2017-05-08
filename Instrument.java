import BIT.highBIT.*;
import BIT.lowBIT.*;
import java.io.*;
import java.util.*;
import java.io.File;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Date;

public class Instrument {
    
    private static PrintStream out = null;
    private static double i_count = 0, b_count = 0, m_count = 0;public static double grand_total = 0;
    public static double const_total = 0;
    public static double field_total = 0;
    public static double interface_total = 0;
    public static double methods_total = 0;
    public static double bytecodes_total = 0;
    public static double bytecodes_partial=0;

    private static double newcount = 0;
    private static double newarraycount = 0;
    private static double anewarraycount = 0;
    private static double multianewarraycount = 0;

    private static double loadcount = 0;
    private static double storecount = 0;
    private static double fieldloadcount = 0;
    private static double fieldstorecount = 0;
    private static String metrics = "";

    
    /* main reads in all the files class files present in the input directory,
     * instruments them, and outputs them to the specified output directory.
     */
    public static void main(String argv[]) throws IOException{
        File file_in = new File(argv[0]);

        getClassesStats(file_in);
        countInstructions(file_in,argv[0]);
        doLoadStore(file_in, file_in);
        doAlloc(file_in, file_in);
    }

    public static void countInstructions(File file_in,String arg){
        String infilenames[] = file_in.list();
        
        for (int i = 0; i < infilenames.length; i++) {
            String infilename = infilenames[i];
            if (infilename.endsWith(".class")) {
                // create class info object
                ClassInfo ci = new ClassInfo(arg + System.getProperty("file.separator") + infilename);
                
                // loop through all the routines
                // see java.util.Enumeration for more information on Enumeration class
                for (Enumeration e = ci.getRoutines().elements(); e.hasMoreElements(); ) {
                    Routine routine = (Routine) e.nextElement();
                    routine.addBefore("Instrument", "countMethods", new Integer(1));
                    
                    for (Enumeration b = routine.getBasicBlocks().elements(); b.hasMoreElements(); ) {
                        BasicBlock bb = (BasicBlock) b.nextElement();
                        bb.addBefore("Instrument", "countInstructions", new Integer(bb.size()));
                    }
                }
                ci.addAfter("Instrument", "printDynamicInfo", ci.getClassName());
                ci.write(arg + System.getProperty("file.separator") + infilename);
            }
        }
    }

    public static void getClassesStats(File file_in){
        try {
            String tmppath = new String(file_in.getAbsolutePath());
            String p = new String(tmppath.substring(0, tmppath.length() - 2));
            processFiles(file_in, p);
            System.out.println("################################# TOTAL STATIC INFORMATION ####################################\n");
            System.err.println("Class size: " + grand_total + "\n" 
                + "Const: " + const_total + "\n"
                + "Fields: "+ field_total + "\n"
                + "Interfaces: " + interface_total + "\n"
                + "Methods: " + methods_total + "\n"
                + "Bytecodes: " +  bytecodes_total + "\n");
            System.out.println("##############################################################################################\n");
        } catch (Exception e) {
            System.err.println("Exception! in main method: " + e);
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(-1);
           }
    }
    
    public static synchronized void printDynamicInfo(String foo) {
        metrics += "########## DYNAMIC INFORMATION #########\n"
            + "Instructions:   " + i_count + "\n" 
            + "Basic blocks:   " + b_count + "\n" 
            + "Methods:        " + m_count + "\n\n";
        //System.out.println("##################################### DYNAMIC INFORMATION ####################################\n");
        //System.out.println("Instructions: " + i_count + "\n" 
        //    + "Basic blocks: " + b_count + "\n" 
        //    + "Methods: " + m_count + "\n");
    }
    

    public static synchronized void countInstructions(int incr) {
        i_count += incr;
        b_count++;
    }

    public static synchronized void countMethods(int incr) {
		m_count++;
    }

    public static void processFiles(File fi, String tmppath) {

       /* recursive method that finds all class files under this directory */
           try {
        String ifnames[] = fi.list();
            for (int i = 0; i < ifnames.length; i++) {

            String tmpstr = new String(tmppath+"/"+ifnames[i]);
                        File file_tmp = new File(tmpstr);
                if (file_tmp.isDirectory() == true) { /* search this directory for class files */
                /* if (file_tmp.isDirectory()) would have worked above as well */

                                //tmppath = new String(file_tmp.getAbsolutePath());
                                //processFiles(file_tmp, tmppath);

            } else { /* see if this is a class file and if so, process it */
            
                String name = new String(tmppath + "/" + ifnames[i]);

                    if (name.endsWith(".class")) {
                        ClassInfo ci = new ClassInfo(name); /* BIT/highBIT call that reads/processes the class */
                        ClassFile cf = ci.getClassFile(); /* BIT/lowBIT call that returns the class file (BIT format)*/

                        grand_total += cf.size();
                        const_total += cf.constant_pool_count  ;
                        field_total += cf.field_count;
                        interface_total += cf.interface_count;
                        methods_total += cf.methods_count;

                        bytecodes_partial=0;
                        for (int i_attr=0; i<cf.attributes_count; i++) {
                            Attribute_Info attr = (Attribute_Info) cf.attributes[i_attr];
                            if (attr instanceof Code_Attribute)
                            {   
//                              System.out.println( "code-size "+((Code_Attribute)attr).code_length);
                                bytecodes_partial+= ((Code_Attribute)attr).code_length;
                            }
                    }
                    bytecodes_total += bytecodes_partial;


                    System.out.println("##################################### STATIC INFORMATION ####################################\n");
                    /* getClassName is in BIT/highBIT/ClassInfo */
                    System.err.println("Class Name: " + ci.getClassName() + "\n" 
                        + "Size:       " + cf.size() + "\n"
                        + "Const:      " + cf.constant_pool_count + "\n"
                        + "Fields:     "+ cf.field_count + "\n"
                        + "Interfaces: " + cf.interface_count + "\n"
                        + "Methods:    " + cf.methods_count + "\n"
                        + "Bytecodes:  " +  bytecodes_partial+"\n");
                    System.out.println("##############################################################################################\n");
                }
            }
        }
        return;
       } catch(Exception e) {
            System.err.println("Exception! in processFiles: " + e);
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(-1);
       }
    }

    public static void doAlloc(File in_dir, File out_dir) 
    {
        String filelist[] = in_dir.list();
        
        for (int i = 0; i < filelist.length; i++) {
            String filename = filelist[i];
            if (filename.endsWith(".class")) {
                String in_filename = in_dir.getAbsolutePath() + System.getProperty("file.separator") + filename;
                String out_filename = out_dir.getAbsolutePath() + System.getProperty("file.separator") + filename;
                ClassInfo ci = new ClassInfo(in_filename);

                for (Enumeration e = ci.getRoutines().elements(); e.hasMoreElements(); ) {
                    Routine routine = (Routine) e.nextElement();
                    InstructionArray instructions = routine.getInstructionArray();
      
                    for (Enumeration instrs = instructions.elements(); instrs.hasMoreElements(); ) {
                        Instruction instr = (Instruction) instrs.nextElement();
                        int opcode=instr.getOpcode();
                        if ((opcode==InstructionTable.NEW) ||
                            (opcode==InstructionTable.newarray) ||
                            (opcode==InstructionTable.anewarray) ||
                            (opcode==InstructionTable.multianewarray)) {
                            instr.addBefore("Instrument", "allocCount", new Integer(opcode));
                        }
                    }
                }
                ci.addAfter("Instrument", "printAlloc", "null");
                ci.write(out_filename);
            }
        }
    }

    public static synchronized void printAlloc(String s) throws IOException
    {
        metrics += "Allocations summary:" + "\n"
            + "new:            " + newcount + "\n"
            + "newarray:       " + newarraycount + "\n"
            + "anewarray:      " + anewarraycount + "\n"
            + "multianewarray: " + multianewarraycount + "\n"
            + "########################################";
       System.out.println(metrics);
        //createLog(metrics);
        metrics = "";
        /*System.out.println("Allocations summary:");
        System.out.println("new:            " + newcount);
        System.out.println("newarray:       " + newarraycount);
        System.out.println("anewarray:      " + anewarraycount);
        System.out.println("multianewarray: " + multianewarraycount);
        System.out.println("##############################################################################################\n");*/
    }

    public static synchronized void allocCount(int type)
    {
        switch(type) {
        case InstructionTable.NEW:
            newcount++;
            break;
        case InstructionTable.newarray:
            newarraycount++;
            break;
        case InstructionTable.anewarray:
            anewarraycount++;
            break;
        case InstructionTable.multianewarray:
            multianewarraycount++;
            break;
        }
    }
    
    public static void doLoadStore(File in_dir, File out_dir) 
    {
        String filelist[] = in_dir.list();
        
        for (int i = 0; i < filelist.length; i++) {
            String filename = filelist[i];
            if (filename.endsWith(".class")) {
                String in_filename = in_dir.getAbsolutePath() + System.getProperty("file.separator") + filename;
                String out_filename = out_dir.getAbsolutePath() + System.getProperty("file.separator") + filename;
                ClassInfo ci = new ClassInfo(in_filename);

                for (Enumeration e = ci.getRoutines().elements(); e.hasMoreElements(); ) {
                    Routine routine = (Routine) e.nextElement();
                    
                    for (Enumeration instrs = (routine.getInstructionArray()).elements(); instrs.hasMoreElements(); ) {
                        Instruction instr = (Instruction) instrs.nextElement();
                        int opcode=instr.getOpcode();
                        if (opcode == InstructionTable.getfield)
                            instr.addBefore("Instrument", "LSFieldCount", new Integer(0));
                        else if (opcode == InstructionTable.putfield)
                            instr.addBefore("Instrument", "LSFieldCount", new Integer(1));
                        else {
                            short instr_type = InstructionTable.InstructionTypeTable[opcode];
                            if (instr_type == InstructionTable.LOAD_INSTRUCTION) {
                                instr.addBefore("Instrument", "LSCount", new Integer(0));
                            }
                            else if (instr_type == InstructionTable.STORE_INSTRUCTION) {
                                instr.addBefore("Instrument", "LSCount", new Integer(1));
                            }
                        }
                    }
                }
                ci.addAfter("Instrument", "printLoadStore", "null");
                ci.write(out_filename);
            }
        }   
    }

    public static synchronized void printLoadStore(String s) 
    {
        metrics += "Load Store Summary:" + "\n" 
            + "Field load:     " + fieldloadcount + "\n"
            + "Field store:    " + fieldstorecount + "\n"
            + "Regular load:   " + loadcount + "\n"
            + "Regular store:  " + storecount + "\n\n";
        /*System.out.println("Load Store Summary:");
        System.out.println("Field load:    " + fieldloadcount);
        System.out.println("Field store:   " + fieldstorecount);
        System.out.println("Regular load:  " + loadcount);
        System.out.println("Regular store: " + storecount + "\n");*/
    }

    public static synchronized void LSFieldCount(int type) 
    {
        if (type == 0)
            fieldloadcount++;
        else
            fieldstorecount++;
    }

    public static synchronized void LSCount(int type) 
    {
        if (type == 0)
            loadcount++;
        else
            storecount++;
    }    
}
