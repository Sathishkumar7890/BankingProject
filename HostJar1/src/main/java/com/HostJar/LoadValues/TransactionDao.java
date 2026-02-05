package com.HostJar.LoadValues;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;




@Component
public class TransactionDao {

    @Autowired
    private DataSource dataSource;

    private static final String INSERT_SQL =
        "INSERT INTO Banking_Transaction_History (" +
        "ucid, clid, dnis, reference_number, start_date, end_date, " +
        "function_name, host_url, host_request, host_response, " +
        "trans_status, http_code, server_ip) " +
        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

    public String insertTransaction(
            String ucid,
            String clid,
            String dnis,
            String functionName,
            String hostUrl,
            String hostRequest,
            String hostResponse,
           int transStatus,
            int httpCode,
            String serverIp) {

        String referenceNumber = UUID.randomUUID().toString();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {

            ps.setString(1, ucid);
            ps.setString(2, clid);
            ps.setString(3, dnis);
            ps.setString(4, referenceNumber);
            ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            ps.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
            ps.setString(7, functionName);
            ps.setString(8, hostUrl);
            ps.setString(9, hostRequest);
            ps.setString(10, hostResponse);
            ps.setInt(11, transStatus);
            ps.setInt(12, httpCode);
            ps.setString(13, serverIp);

            ps.executeUpdate();
            return referenceNumber;

        } catch (Exception e) {
            throw new RuntimeException("Failed to insert transaction log", e);
        }
        
        
    }
   
}
