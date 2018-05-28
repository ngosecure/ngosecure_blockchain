package ngosecure.util;


import ngosecure.vo.NGOTransactionHeader;
import ngosecure.vo.NGOTransactionReport;
import org.beanio.BeanReader;
import org.beanio.BeanWriter;
import org.beanio.StreamFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NGOSecureUtil {

    private static final String REPORT_OUTPUT_PATH = "C:/BlockChain/Output";
    private static final String REPORT_FILE_NAME = "NGOTransactionReport";
    private static final String REPORT_MIME_TYPE_EXT = ".csv";
    private static final String REPORT_RECORD_STREAM_NAME = "ngotransactionreport";

    public static void main(String[] args) {
     //   buildNGOTransactionReport();
    }

    public void buildNGOTransactionReport(NGOTransactionReport ngoTransactionReport){
        BeanWriter beanWriter;
        BeanReader beanReader;
        StreamFactory streamFactory = StreamFactory.newInstance();
        streamFactory.load( "C:\\BlockChain\\ngosecure_blockchain\\builder\\NGOTransactionReport.xml");
        System.out.println("Bean io xml loaded");
        Boolean reportFileExists = new File(REPORT_OUTPUT_PATH, REPORT_FILE_NAME +
                REPORT_MIME_TYPE_EXT).exists();
        File reportFile;
        File outputDir = new File(REPORT_OUTPUT_PATH);
        if(!outputDir.exists()){
            outputDir.mkdir();
        }
        if(!reportFileExists){
            reportFile = new File(REPORT_OUTPUT_PATH, REPORT_FILE_NAME +
                    REPORT_MIME_TYPE_EXT);
            beanWriter = streamFactory.createWriter(REPORT_RECORD_STREAM_NAME,reportFile);
            writeReportHeader(beanWriter);
            writeTransactionRecord(beanWriter,ngoTransactionReport);
        }else{
            reportFile = new File(REPORT_OUTPUT_PATH + "/" + REPORT_FILE_NAME + REPORT_MIME_TYPE_EXT);
            File existingReportFile = new File(REPORT_OUTPUT_PATH + "/" + REPORT_FILE_NAME + "_old" + REPORT_MIME_TYPE_EXT);

            reportFile.renameTo(existingReportFile);
            existingReportFile.setReadable(true);

            beanReader = streamFactory.createReader(REPORT_RECORD_STREAM_NAME,existingReportFile);
            reportFile = new File(REPORT_OUTPUT_PATH + "/" + REPORT_FILE_NAME + REPORT_MIME_TYPE_EXT);
            beanWriter = streamFactory.createWriter(REPORT_RECORD_STREAM_NAME,reportFile);

            Object existingRecord;

            while ((existingRecord = beanReader.read()) != null) {
                if(existingRecord instanceof NGOTransactionHeader){
                    System.out.println(((NGOTransactionHeader) existingRecord).getAMOUNT());
                }else if(existingRecord instanceof NGOTransactionReport){
                    System.out.println(((NGOTransactionReport) existingRecord).getAMOUNT());
                }
                beanWriter.write(existingRecord);
            }
            beanReader.close();
            writeTransactionRecord(beanWriter,ngoTransactionReport);
            existingReportFile.delete();
        }
        beanWriter.flush();
        beanWriter.close();
        System.out.println("Report file created");
    }

    public List<NGOTransactionReport> retrieveLedgerTransactions(String partyName){
        ArrayList<NGOTransactionReport> ngoTransactionReportList = new ArrayList<NGOTransactionReport>();
        BeanReader beanReader;
        StreamFactory streamFactory = StreamFactory.newInstance();
        streamFactory.load(System.getProperty("user.dir") +
                "\\java-source\\src\\main\\java\\ngosecure\\builder\\NGOTransactionReport.xml");
        System.out.println("Bean io xml loaded");
        Boolean reportFileExists = new File(REPORT_OUTPUT_PATH, REPORT_FILE_NAME +
                REPORT_MIME_TYPE_EXT).exists();
        File reportFile;

        if(reportFileExists){
            reportFile = new File(REPORT_OUTPUT_PATH + "/" + REPORT_FILE_NAME + REPORT_MIME_TYPE_EXT);
            beanReader = streamFactory.createReader(REPORT_RECORD_STREAM_NAME,reportFile);

            Object existingRecord;

            while ((existingRecord = beanReader.read()) != null) {
                if(existingRecord instanceof NGOTransactionReport){
                    if(((NGOTransactionReport) existingRecord).getNOTARY().equalsIgnoreCase(partyName)){
                        ngoTransactionReportList.add((NGOTransactionReport) existingRecord);
                    }else if(((NGOTransactionReport) existingRecord).getDONOR().equalsIgnoreCase(partyName) ||
                            ((NGOTransactionReport) existingRecord).getORGANIZATION().equalsIgnoreCase(partyName)){
                        ngoTransactionReportList.add((NGOTransactionReport) existingRecord);
                    }
                }
            }
            beanReader.close();
        }else{
            throw new IllegalStateException("Ledger not available");
        }
        return ngoTransactionReportList;
    }

    private static void buildNGOTransactionReport(){
        BeanWriter beanWriter;
        BeanReader beanReader;
        StreamFactory streamFactory = StreamFactory.newInstance();
        streamFactory.load(System.getProperty("user.dir") +
                "\\java-source\\src\\main\\java\\ngosecure\\builder\\NGOTransactionReport.xml");
        System.out.println("Bean io xml loaded");
        Boolean reportFileExists = new File(REPORT_OUTPUT_PATH, REPORT_FILE_NAME +
                REPORT_MIME_TYPE_EXT).exists();
        File reportFile;
        File existingReportFile;
        if(!reportFileExists){
            reportFile = new File(REPORT_OUTPUT_PATH, REPORT_FILE_NAME +
                    REPORT_MIME_TYPE_EXT);
            beanWriter = streamFactory.createWriter(REPORT_RECORD_STREAM_NAME,reportFile);
            writeReportHeader(beanWriter);
            writeTransactionRecord(beanWriter);
        }else{
            reportFile = new File(REPORT_OUTPUT_PATH + "/" + REPORT_FILE_NAME + REPORT_MIME_TYPE_EXT);
            existingReportFile = new File(REPORT_OUTPUT_PATH + "/" + REPORT_FILE_NAME + "_old" + REPORT_MIME_TYPE_EXT);
            reportFile.renameTo(existingReportFile);

            beanReader = streamFactory.createReader(REPORT_RECORD_STREAM_NAME,existingReportFile);
            reportFile = new File(REPORT_OUTPUT_PATH + "/" + REPORT_FILE_NAME + REPORT_MIME_TYPE_EXT);
            beanWriter = streamFactory.createWriter(REPORT_RECORD_STREAM_NAME,reportFile);

            Object existingRecord;

            while ((existingRecord = beanReader.read()) != null) {
                if(existingRecord instanceof NGOTransactionHeader){
                    System.out.println(((NGOTransactionHeader) existingRecord).getAMOUNT());
                }else if(existingRecord instanceof NGOTransactionReport){
                    System.out.println(((NGOTransactionReport) existingRecord).getAMOUNT());
                }
                beanWriter.write(existingRecord);
            }
            beanReader.close();
            writeTransactionRecord(beanWriter);
            existingReportFile.delete();
        }
        beanWriter.flush();
        beanWriter.close();
        System.out.println("Report file created");
    }

    private static void writeReportHeader(BeanWriter beanWriter) {
        NGOTransactionHeader transactionHeader = new NGOTransactionHeader();

        beanWriter.write(transactionHeader);
    }

    private static void writeTransactionRecord(BeanWriter beanWriter,NGOTransactionReport ngoTransactionReport){
        beanWriter.write(ngoTransactionReport);

    }

    private static void writeTransactionRecord(BeanWriter beanWriter){
        NGOTransactionReport ngoTransactionReport = new NGOTransactionReport("US",
                "FL","NGO Notary","1274000","NGO R","Issuance",new Date().
                toString(),"Part A");
        beanWriter.write(ngoTransactionReport);

    }
}
