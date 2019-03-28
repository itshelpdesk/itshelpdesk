package com.itshelpdesk.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.itshelpdesk.model.BarChartRawDataItem;
import com.pc.custom.exceptions.InternalServerException;

@Repository("dashboardDaoImpl")
public class DashboardDaoImpl implements DashboardDao {

	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Override
	public Integer fetchCountOfTicketsInLastHourByStatus(String ticketStatus) {

		LOGGER.debug("Fetching {} ticket-count in the last one hour", ticketStatus);
		StringBuilder sql = new StringBuilder();

		switch (ticketStatus) {
		case "New":
			sql.append("SELECT * FROM viewnewticketsinlasthour");
			break;
		case "Closed":
			sql.append("SELECT * FROM viewclosedticketsinlasthour");
			break;
		default:
			break;
		}

		Integer count = jdbcTemplate.queryForObject(sql.toString(), new IntegerRowMapper());
		LOGGER.debug("Number of {} tickets in last one hour: {}", ticketStatus, count);

		return count;

	}

	@Override
	public List<Map<Integer, List<Map<String, Integer>>>> fetchTicketCountStatusAndMonthWise() {
		LOGGER.debug("Fetching ticket-count by month and status wise");
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM viewticketsbymonthandstatus");

		List<BarChartRawDataItem> barChartRawData = jdbcTemplate.query(sql.toString(),
				new BarChartRawDataItemRowMapper());
		LOGGER.debug("Fetched BarChartRawData: {}", barChartRawData.toString());

		// Processing the barChartRawData to return in hierarchical map
		List<Map<Integer, List<Map<String, Integer>>>> processedBarChartData = new ArrayList<Map<Integer, List<Map<String, Integer>>>>();

		// Extracting months from barChartData into a set
		Set<Integer> monthSet = new HashSet<Integer>();
		barChartRawData.forEach(item -> {
			monthSet.add(item.getMonth());
		});
		LOGGER.debug("Extracted set of months: {}", monthSet.toString());

		// Loop through the set of months
		monthSet.forEach(month -> {
			// Extract Month-Status-TicketCount list for looping month
			LOGGER.debug("Looping through month: {}", month.toString());
			List<BarChartRawDataItem> barChartRawDataForGivenMonth = barChartRawData.stream()
					.filter(barChartRawDataItem -> {
						return barChartRawDataItem.getMonth().equals(month);
					}).collect(Collectors.toList());
			LOGGER.debug("Extracted Month-Status-TicketCount list for a given month: {}",
					barChartRawDataForGivenMonth.toString());

			// Create List<Map<String, Integer>> for Status-TicketCount pair list from the
			// above list
			Map<String, Integer> statusTktCountMap = new HashMap<String, Integer>();
			List<Map<String, Integer>> statusTktCountMapList = new ArrayList<Map<String, Integer>>();
			barChartRawDataForGivenMonth.forEach(barChartRawDataItem -> {
				statusTktCountMap.put(barChartRawDataItem.getStatus(), barChartRawDataItem.getTicketCount());
				statusTktCountMapList.add(statusTktCountMap);
			});
			LOGGER.debug("Created list of map of status-ticket pair {} for the given month {}", statusTktCountMapList.toString(),
					month.toString());

			// Create Map<Month, List<Status, TicketCount>>
			Map<Integer, List<Map<String, Integer>>> monthStatusTktCountMap = new HashMap<Integer, List<Map<String, Integer>>>();
			monthStatusTktCountMap.put(month, statusTktCountMapList);
			LOGGER.debug("Created map of Month, Statu-Ticket: {}", monthStatusTktCountMap.toString());

			processedBarChartData.add(monthStatusTktCountMap);
		});
		
		LOGGER.debug("Processed BarChartData: {}", processedBarChartData.toString());

		return processedBarChartData;
	}

	private class BarChartRawDataItemRowMapper implements RowMapper<BarChartRawDataItem> {

		@Override
		public BarChartRawDataItem mapRow(ResultSet rs, int rowNum) throws SQLException {
			BarChartRawDataItem barChartRawDataItem = new BarChartRawDataItem();
			barChartRawDataItem.setMonth(rs.getInt("month"));
			barChartRawDataItem.setStatus(rs.getString("sts_name"));
			barChartRawDataItem.setTicketCount(rs.getInt("tkt_count"));

			return barChartRawDataItem;
		}

	}

	private class IntegerRowMapper implements RowMapper<Integer> {

		@Override
		public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
			try {
				if (rs.findColumn("new_ticket_count_last_hour") > 0)
					return new Integer(rs.getInt("new_ticket_count_last_hour"));

				if (rs.findColumn("closed_ticket_count_last_hour") > 0)
					return new Integer(rs.getInt("closed_ticket_count_last_hour"));
			} catch (Exception ex) {

			}

			throw new InternalServerException("An unexpected exception occured");

		}

	}

}