from krausening.properties import PropertyManager


class TestConfig:
    def __init__(self) -> None:
        self.properties = PropertyManager.get_instance().get_properties(
            "test.properties"
        )

    @property
    def baz(self) -> str:
        return self.properties.getProperty("baz", "default")
