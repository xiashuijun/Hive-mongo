package ssy.dmp.hive.mongo;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hive.ql.io.HiveInputFormat;
import org.apache.hadoop.hive.ql.plan.ExprNodeDesc;
import org.apache.hadoop.hive.ql.plan.TableScanDesc;
import org.apache.hadoop.hive.serde2.ColumnProjectionUtils;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;

/*
 * Defines a HiveInputFormat for use in reading data from MongoDB into a hive table
 *
 */
public class MongoInputFormat extends
        HiveInputFormat<LongWritable, MapWritable> {

    @Override
    public RecordReader<LongWritable, MapWritable> getRecordReader(
            InputSplit split, JobConf conf, Reporter reporter)
            throws IOException {
        /*
        Iterator<Entry<String, String>> it = conf.iterator();
		while(it.hasNext()){
			System.err.println(it.next());
		}*/

        List<Integer> readColIDs = ColumnProjectionUtils.getReadColumnIDs(conf);

        boolean addAll = (readColIDs.size() == 0);

        String columnString = conf.get(ConfigurationUtil.COLUMN_MAPPING);
        if (StringUtils.isBlank(columnString)) {
            throw new IOException("no column mapping found!");
        }

        String[] columns = ConfigurationUtil.getAllColumns(columnString);
        if (readColIDs.size() > columns.length) {
            throw new IOException(
                    "read column count larger than that in column mapping string!");
        }

        String[] cols;
        if (addAll) {
            cols = columns;
        } else {
            cols = new String[readColIDs.size()];
            for (int i = 0; i < cols.length; i++) {
                cols[i] = columns[readColIDs.get(i)];
            }
        }

        String filterExprSerialized =
                conf.get(TableScanDesc.FILTER_EXPR_CONF_STR);

        if (filterExprSerialized != null) {
            //System.err.println("=======filter expr is " + filterExprSerialized);
            //ExprNodeDesc filterExpr =
            //  Utilities.deserializeExpression(filterExprSerialized, conf);
	    	  /*String columnNameProperty = conf.get(
	    		      org.apache.hadoop.hive.serde.Constants.LIST_COLUMNS);
	    	  System.err.println("======list columns:" + columnNameProperty);*/
            //dumpFilterExpr(filterExpr);
            //TODO:
        }

        return new MongoReader(ConfigurationUtil.getDBHost(conf), ConfigurationUtil.getDBPort(conf),
                ConfigurationUtil.getDBName(conf), ConfigurationUtil.getDBUser(conf), ConfigurationUtil.getDBPassword(conf), ConfigurationUtil.getCollectionName(conf),
                (MongoSplit) split, cols);
    }

    @Override
    public InputSplit[] getSplits(JobConf conf, int numSplits)
            throws IOException {
        return MongoSplit.getSplits(conf, ConfigurationUtil.getDBHost(conf), ConfigurationUtil.getDBPort(conf),
                ConfigurationUtil.getDBName(conf), ConfigurationUtil.getDBUser(conf), ConfigurationUtil.getDBPassword(conf), ConfigurationUtil.getCollectionName(conf), numSplits);
    }


    void dumpFilterExpr(ExprNodeDesc expr) {
        if (expr == null) return;
        System.err.println("cols=" + expr.getCols() + ", expr=" + expr.getExprString());
        List<ExprNodeDesc> children = expr.getChildren();
        if (children != null && children.size() > 0) {
            System.err.println("children=");
            for (ExprNodeDesc e : children) {
                dumpFilterExpr(e);
            }
        }
    }
}
