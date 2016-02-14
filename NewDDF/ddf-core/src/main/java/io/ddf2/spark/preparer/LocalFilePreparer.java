package io.ddf2.spark.preparer;

import io.ddf2.datasource.PrepareDataSourceException;
import io.ddf2.datasource.filesystem.fileformat.CSVFile;
import io.ddf2.datasource.filesystem.LocalFileDataSource;
import io.ddf2.datasource.filesystem.fileformat.IFileFormat;
import io.ddf2.datasource.filesystem.fileformat.JSonFile;
import io.ddf2.datasource.filesystem.fileformat.ParquetFile;
import io.ddf2.datasource.schema.IColumn;
import io.ddf2.datasource.schema.ISchema;
import io.ddf2.spark.SparkUtils;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.hive.HiveContext;
import scala.collection.Seq;

import java.util.List;

/**
 * Created by sangdn on 1/8/16.
 * LocalFilePreparer will work on Local File System to prepare data
 * Prepare Data = Create Hive Table -> Load all local file to table
 */
public class LocalFilePreparer extends SparkFilePreparer {
    public LocalFilePreparer(HiveContext hiveContext) {
        super( hiveContext);
    }


    @Override
    protected void prepareData(String ddfName, ISchema schema, LocalFileDataSource fileDataSource) throws PrepareDataSourceException {
        //Create Table name with Schema
        IFileFormat fileFormat = fileDataSource.getFileFormat();
        if(fileFormat instanceof CSVFile){
            prepareCSV(ddfName,schema,fileDataSource);
        }else if(fileFormat instanceof JSonFile){
            prepareJSON(ddfName,schema,fileDataSource);
        }else if(fileFormat instanceof ParquetFile){
            prepareParquet(ddfName,schema,fileDataSource);
        }else{
            throw new PrepareDataSourceException(fileFormat.getClass().getSimpleName() + "Unsupported !");
        }
    }

    private void prepareParquet(String ddfName, ISchema schema, LocalFileDataSource fileDataSource) {
        String[] paths = fileDataSource.getPaths().toArray(new String[0]);
        hiveContext.read().parquet(paths).registerTempTable(ddfName);

    }

    private void prepareJSON(String ddfName, ISchema schema, LocalFileDataSource fileDataSource) {
        String paths = String.join(",", fileDataSource.getPaths());
        DataFrame dataFrame = hiveContext.jsonFile(paths);
        dataFrame.registerTempTable(ddfName);
    }

    private void prepareCSV(String ddfName, ISchema schema, LocalFileDataSource fileDataSource) throws PrepareDataSourceException {
        List<IColumn> columns = schema.getColumns();
        CSVFile csvFormat = (CSVFile)fileDataSource.getFileFormat();
        StringBuilder strCreateTable = new StringBuilder();
        strCreateTable.append("CREATE TABLE ").append(ddfName).append(" ( ");
        StringBuilder sbTableSchema = new StringBuilder();
        for (int i = 0; i < columns.size(); ++i) {
            if (sbTableSchema.length() > 0) sbTableSchema.append(",");
            String name = columns.get(i).getName();
            Class javaType = columns.get(i).getType();
            String hiveTypeName = SparkUtils.javaTypeToHiveName(javaType);

            sbTableSchema.append(name).append(" ").append(hiveTypeName);
        }
        strCreateTable.append(sbTableSchema.toString()).append(" )");
        strCreateTable.append(" ROW FORMAT DELIMITED FIELDS TERMINATED BY '" + csvFormat.getDelimiter() + "'");

        if (csvFormat.firstRowIsHeader())
            strCreateTable.append(" tblproperties('skip.header.line.count'='1')");
        hiveContext.sql(strCreateTable.toString());

        //load local data from paths
        for (String path : fileDataSource.getPaths()) {
            String strLoadData = "load data local inpath '" + path + "' into table " + ddfName;
            hiveContext.sql(strLoadData);
        }
    }


}