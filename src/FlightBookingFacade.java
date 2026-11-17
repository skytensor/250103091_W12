public class FlightBookingFacade {
    private final SeatInventorySystem seatInventorySystem;
    private final BaggagePricingEngine baggagePricingEngine;
    private final CurrencyConverter currencyConverter;
    private final PaymentProcessor paymentProcessor;
    private final TicketIssuer ticketIssuer;

    public FlightBookingFacade() {
        this.seatInventorySystem = new SeatInventorySystem();
        this.baggagePricingEngine = new BaggagePricingEngine();
        this.currencyConverter = new CurrencyConverter();
        this.paymentProcessor = new PaymentProcessor();
        this.ticketIssuer = new TicketIssuer();
    }

    public String bookFlight(String passengerName,
                             String flightCode,
                             String seatNumber,
                             double baseFareUSD,
                             int bagCount,
                             double bagWeightKg,
                             String currency,
                             String cardNumber) {

        String lockToken = null;

        try {
            seatInventorySystem.connectToReservationServer();
            lockToken = seatInventorySystem.lockSeat(flightCode, seatNumber, passengerName);
            double totalUSD = baggagePricingEngine.calculateBaggageFeeUSD(bagCount, bagWeightKg) + baseFareUSD;
            double convertedTotal = currencyConverter.convertFromUSD(totalUSD, currency);

            paymentProcessor.initializeGateway();
            String receiptId = paymentProcessor.chargeCard(cardNumber, convertedTotal, currency);

            return ticketIssuer.generateTicket(passengerName, flightCode, lockToken, receiptId);
        }
        catch (RuntimeException e) {
            if (lockToken != null) {
                seatInventorySystem.releaseSeatLock(lockToken);
            }
            return "BOOKING_FAILED: " + e.getMessage();
        }
        finally {
            seatInventorySystem.disconnect();
        }

    }
}
