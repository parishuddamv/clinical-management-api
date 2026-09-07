import uuid


class PaymentProviderMock:
    @staticmethod
    def create_funding_intent(gift_id: str, amount: int, currency: str) -> str:
        return f"pay_{gift_id[:8]}_{currency}_{amount}_{uuid.uuid4().hex[:8]}"


class PayoutProviderMock:
    @staticmethod
    def create_payout(gift_id: str, upi_id: str, amount: int, idempotency_key: str) -> str:
        return f"payout_{gift_id[:8]}_{upi_id.split('@')[0]}_{idempotency_key[:8]}"
