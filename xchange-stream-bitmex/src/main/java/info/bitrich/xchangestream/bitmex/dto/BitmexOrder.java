package info.bitrich.xchangestream.bitmex.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;

import java.math.BigDecimal;

public class BitmexOrder extends BitmexMarketDataEvent {

  public enum OrderStatus {
    NEW,
    PARTIALLYFILLED,
    FILLED,
    TBD,
    CANCELED,
    REJECTED,
    UNKNOW
  }

  private String orderID;

  private int account;

  private String side;

  private BigDecimal price;

  private BigDecimal avgPx;

  private String ordType;

  private OrderStatus ordStatus;

  private String clOrdID;

  private BigDecimal orderQty;

  private BigDecimal cumQty;

  public boolean isNotWorkingIndicator() {
    return !workingIndicator;
  }

  private boolean workingIndicator;

  @JsonCreator
  public BitmexOrder(
      @JsonProperty("symbol") String symbol,
      @JsonProperty("timestamp") String timestamp,
      @JsonProperty("orderID") String orderID,
      @JsonProperty("account") int account,
      @JsonProperty("side") String side,
      @JsonProperty("price") BigDecimal price,
      @JsonProperty("avgPx") BigDecimal avgPx,
      @JsonProperty("ordType") String ordType,
      @JsonProperty("ordStatus") String ordStatus,
      @JsonProperty("clOrdID") String clOrdID,
      @JsonProperty("orderQty") BigDecimal orderQty,
      @JsonProperty("cumQty") BigDecimal cumQty,
      @JsonProperty("workingIndicator") boolean workingIndicator) {
    super(symbol, timestamp);
    this.orderID = orderID;
    this.account = account;
    this.side = side;
    this.price = price;
    this.avgPx = avgPx;
    this.ordType = ordType;
    try {
      this.ordStatus = OrderStatus.valueOf(ordStatus.toUpperCase());
    } catch (Exception e) {
      this.ordStatus = OrderStatus.UNKNOW;
    }
    this.clOrdID = clOrdID;
    this.orderQty = orderQty;
    this.cumQty = cumQty;
    this.workingIndicator = workingIndicator;
  }

  public Order toOrder() {
    Order.Builder order;
    if (ordType == null && BitmexOrder.OrderStatus.CANCELED.equals(ordStatus)) {
      order = new BitmexCancelOrder.BitmexCancelOrderBuilder(getInstrument());
    } else if ("Market".equals(ordType)) {
      order =
              new MarketOrder.Builder(
                      "Buy".equals(side) ? Order.OrderType.BID : Order.OrderType.ASK,
                      getInstrument());
    } else {
      order =
              new LimitOrder.Builder(
                      "Buy".equals(side) ? Order.OrderType.BID : Order.OrderType.ASK,
                      getInstrument());
    }
    order.id(orderID).averagePrice(avgPx).originalAmount(orderQty).cumulativeAmount(cumQty).timestamp(getDate()).userReference(clOrdID);

    switch (ordStatus) {
      case NEW:
        order.orderStatus(Order.OrderStatus.NEW);
        break;
      case PARTIALLYFILLED:
        order.orderStatus(Order.OrderStatus.PARTIALLY_FILLED);
        break;
      case FILLED:
        order.orderStatus(Order.OrderStatus.FILLED);
        break;
      case TBD:
        order.orderStatus(Order.OrderStatus.PENDING_CANCEL);
        break;
      case CANCELED:
        order.orderStatus(Order.OrderStatus.CANCELED);
        break;
      case REJECTED:
        order.orderStatus(Order.OrderStatus.REJECTED);
      default:
        order.orderStatus(Order.OrderStatus.UNKNOWN);
        break;
    }

    return order.build();
  }

  private CurrencyPair getInstrument() {
    return new CurrencyPair(symbol.substring(0, 3), symbol.substring(3));
  }

  public String getOrderID() {
    return orderID;
  }

  public int getAccount() {
    return account;
  }

  public String getSide() {
    return side;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public BigDecimal getAvgPx() {
    return avgPx;
  }

  public String getOrdType() {
    return ordType;
  }

  public OrderStatus getOrdStatus() {
    return ordStatus;
  }

  public String getClOrdID() {
    return clOrdID;
  }

  public BigDecimal getOrderQty() {
    return orderQty;
  }

  public BigDecimal getCumQty() {
    return cumQty;
  }

  public boolean isWorkingIndicator() {
    return workingIndicator;
  }
}
