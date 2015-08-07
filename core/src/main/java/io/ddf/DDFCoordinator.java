package io.ddf;

import com.google.common.base.Strings;
import io.ddf.content.SqlResult;
import io.ddf.datasource.DataSourceDescriptor;
import io.ddf.datasource.JDBCDataSourceCredentials;
import io.ddf.datasource.JDBCDataSourceDescriptor;
import io.ddf.exception.DDFException;
import io.ddf.misc.Config;
import io.ddf.util.ConfigHandler;

import java.net.URISyntaxException;
import java.util.*;

/**
 * Created by jing on 7/23/15.
 */
public class DDFCoordinator {
    // The ddfmangers.
    private List<DDFManager> mDDFManagerList
            = new ArrayList<DDFManager>();
    // The mapping from engine name to ddfmanager.
    private Map<UUID, DDFManager> mUuid2DDFManager
            = new HashMap<UUID, DDFManager>();
    private Map<String, DDFManager> mURI2DDFManager
            = new HashMap<String, DDFManager>();
    private Map<String, DDFManager> mName2DDFManager
            = new HashMap<String, DDFManager>();
    // The default engine.
    private String mDefaultEngine;
    private String mComputeEngine;


    public DDFCoordinator(Boolean test) {

    }

    public DDFCoordinator() throws DDFException, URISyntaxException {
        if (mDDFManagerList.isEmpty()) {
            String engineType = Config.getGlobalValue(Config.ConfigConstant
                    .FIELD_ENGINE_TYPE);
            switch (engineType) {
                case "spark": {
                    DDFManager ddfManager = DDFManager.get("spark");
                    ddfManager.setEngineName("spark");
                    ddfManager.setDDFCoordinator(this);
                    this.getDDFManagerList().add(ddfManager);
                    this.getName2DDFManager().put("spark", ddfManager);
                    break;
                }
                case "jdbc": {
                    String user = Config.getGlobalValue(Config.ConfigConstant
                            .FIELD_USER);
                    String password = Config.getGlobalValue(Config
                            .ConfigConstant.FIELD_PASSWORD);
                    String url = Config.getGlobalValue(Config.ConfigConstant
                            .FIELD_URL);
                    String engineName = Config.getGlobalValue(Config
                            .ConfigConstant.FIELD_ENGINE);
                    mDefaultEngine = engineName;
                    JDBCDataSourceDescriptor jdbcDS = new
                            JDBCDataSourceDescriptor(url, user, password, null);
                    DDFManager ddfManager = DDFManager.get("jdbc", jdbcDS);
                    ddfManager.setEngineName(engineName);
                    ddfManager.setDDFCoordinator(this);
                    this.getDDFManagerList().add(ddfManager);
                    this.getName2DDFManager().put(engineName, ddfManager);
                    break;
                }
            }
        }
    }

    public String getDefaultEngine() {
        if (mDefaultEngine == null) {
            mDefaultEngine = "spark";
        }
        return mDefaultEngine;
    }

    public void setURI2DDFManager(String uri, DDFManager ddfManager) {
        ddfManager.log("Set uri2ddfManager with uri: " + uri);
        mURI2DDFManager.put(uri, ddfManager);
    }

    public DDFManager getDDFManagerByURI(String uri) throws DDFException {
        DDFManager dm =  mURI2DDFManager.get(uri);
        if (dm == null) {
            throw new DDFException("Can't find ddfmanager for ddf: " + uri);
        }
        return dm;
    }

    public void setUuid2DDFManager(UUID uuid, DDFManager ddfManager) {
        mUuid2DDFManager.put(uuid, ddfManager);
    }

    public DDFManager getDDFManagerByUUID(UUID uuid) {
        return mUuid2DDFManager.get(uuid);
    }

    public void setDefaultEngine(String defaultEngine) {
        this.mDefaultEngine = defaultEngine;
    }

    public String getComputeEngine() {
        return mComputeEngine;
    }

    public void setComputeEngine(String computeEngine) {
        mComputeEngine = computeEngine;
    }

    public Map<String, DDFManager> getName2DDFManager() {
        return mName2DDFManager;
    }

    public void setName2DDFManager(Map<String, DDFManager> mName2DDFManager) {
        this.mName2DDFManager = mName2DDFManager;
    }

    public List<DDFManager> getDDFManagerList() {
        return mDDFManagerList;
    }

    public void setDDFManagerList(List<DDFManager> mDDFManagerList) {
        this.mDDFManagerList = mDDFManagerList;
    }

    public List<String> showEngines() {
        List<String> ret = new ArrayList<String>();
        for (DDFManager ddfManager : this.mDDFManagerList) {
            ret.add(ddfManager.getEngineName());
        }
        return ret;
    }

    public DDF getDDF(UUID uuid) {
        for (DDFManager ddfManager : mDDFManagerList) {
            try {
                if (ddfManager.getDDF(uuid) != null) {
                    return ddfManager.getDDF(uuid);
                }
            } catch (DDFException e) {
                // e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * @brief Init an engine.
     * @param engineName The unique name of the engine.
     * @param engineType The type of the engine.
     * @param dataSourceDescriptor DataSource.
     * @return The new ddf manager.
     * @throws DDFException
     */
    public DDFManager initEngine(String engineName, String engineType,
                                 DataSourceDescriptor dataSourceDescriptor)
            throws DDFException {
        if (engineName == null) {
            throw new DDFException("Please input engine name");
        }
        if (mName2DDFManager.get(engineName) != null) {
            throw new DDFException("This engine name is already used : " +
                    engineName);
        }
        DDFManager manager = DDFManager.get(engineType, dataSourceDescriptor);
        if (manager == null) {
            throw new DDFException("Error int get the DDFManager for engine :" +
                    engineName);
        }
        manager.setEngineName(engineName);
        manager.setDDFCoordinator(this);
        mDDFManagerList.add(manager);
        mName2DDFManager.put(engineName, manager);
        return manager;
    }

    public int stopEngine(String engineName) {
        return 0;
    }

    /**
     * @brief Browse what content is in the engine.
     * @param engineName the engine name.
     * @return The "show tables" result.
     * @throws DDFException
     */
    public SqlResult browseEngine(String engineName) throws DDFException {
        DDFManager ddfManager = this.getEngine(engineName);
        return ddfManager.sql("show tables", ddfManager.getEngine());
    }

    /**
     * @brief Browse ddfs in all engines.
     * @return
     * @throws DDFException
     */
    public List<SqlResult> browseEngines() throws DDFException {
        List<SqlResult> retList = new ArrayList<SqlResult>();
        for (String engineName : mName2DDFManager.keySet()) {
            retList.add(this.browseEngine(engineName));
        }
        return retList;
    }

    /**
     * @brief Get the engine with given name.
     * @param engineName The name of the engine.
     * @return The ddfmanager, or null if there is no such engine.
     * @throws DDFException
     */
    public DDFManager getEngine(String engineName) throws DDFException {
        DDFManager manager =  mName2DDFManager.get(engineName);
        if (manager == null) {
            throw new DDFException("There is no engine with name : " +
                    engineName);
        }
        return manager;
    }

    public DDF sql2ddf(String sqlCmd, String dataSource) {
        return null;
    }

    public DDF sql2ddf(String sqlCmd, DataSourceDescriptor dataSourceDescriptor) {
        return null;
    }

    /**
     * @brief Run the sql command using default compute engine.
     * @param sqlcmd The command.
     * @return The sql result.
     */
    public SqlResult sqlX(String sqlcmd) throws DDFException {
        if (this.getDefaultEngine() == null) {
            throw new DDFException("No default engine specified, please " +
                    "specify the default engine or pass the engine name");
        }
        return this.sqlX(sqlcmd, this.getDefaultEngine());
    }

    /**
     * @brief Run the sql command on the given enginename.
     * @param sqlcmd
     * @param computeEngine
     * @return
     */
    public SqlResult sqlX(String sqlcmd, String computeEngine) throws DDFException {
        DDFManager defaultManager = this.getEngine(computeEngine);
        return defaultManager.sql(sqlcmd);
    }

    public DDF sql2ddfX(String sqlcmd) throws DDFException {
        if (this.getDefaultEngine() == null) {
            throw new DDFException("No default engine specified, please " +
                    "specify the default engine or pass the engine name");
        }
        return this.sql2ddf(sqlcmd, this.getDefaultEngine());
    }

    public DDF sql2ddfX(String sqlcmd, String computeEngine) throws
            DDFException {
        DDFManager defaultManager = this.getEngine(computeEngine);
        return defaultManager.sql2ddf(sqlcmd);
    }

    public DDF transfer(String fromEngine, String toEngine, String ddfuri) throws DDFException {
        DDFManager defaultManager = this.getEngine(toEngine);
        return defaultManager.transfer(fromEngine, ddfuri);
    }
}
