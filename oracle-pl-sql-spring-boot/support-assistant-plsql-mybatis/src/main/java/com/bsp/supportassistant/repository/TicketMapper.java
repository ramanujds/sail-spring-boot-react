package com.bsp.supportassistant.repository;

import com.bsp.supportassistant.dto.TicketResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.StatementType;

import java.util.List;
import java.util.Map;

// One method for each program of the package. MyBatis creates the code of the methods.
// @Select holds the call. CALLABLE tells MyBatis that it is a call to a stored program, not a query.
// #{name} is replaced by the value of the argument with that name.
@Mapper
public interface TicketMapper {

    // A function: called from a SELECT. The return value is the result.
    @Select("SELECT SUPPORT_TICKET_PKG.open_count(#{priority}) FROM DUAL")
    int openCount(String priority);

    // mode=OUT marks the OUT parameter. MyBatis puts its value in the Map, under the name ticketId.
    @Select("{call SUPPORT_TICKET_PKG.create_ticket(#{title}, #{description}, #{priority},"
            + " #{ticketId, mode=OUT, jdbcType=NUMERIC})}")
    @Options(statementType = StatementType.CALLABLE)
    void createTicket(Map<String, Object> params);

    @Select("{call SUPPORT_TICKET_PKG.change_status(#{ticketId}, #{newStatus})}")
    @Options(statementType = StatementType.CALLABLE)
    void changeStatus(@Param("ticketId") long ticketId, @Param("newStatus") String newStatus);

    // jdbcType=CURSOR marks the SYS_REFCURSOR. Each row becomes a TicketResponse, as @Results ticketMap says.
    // The rows arrive in the Map, under the name tickets. The columns of the SELECT in find_tickets
    // must be in the same order as the fields of TicketResponse.
    @Select("{call SUPPORT_TICKET_PKG.find_tickets(#{status},"
            + " #{tickets, mode=OUT, jdbcType=CURSOR, javaType=java.sql.ResultSet, resultMap=ticketMap})}")
    @Options(statementType = StatementType.CALLABLE)
    @Results(id = "ticketMap", value = {})
    List<TicketResponse> findTickets(Map<String, Object> params);
}
