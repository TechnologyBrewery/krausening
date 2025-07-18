import os
import shutil


def before_scenario(context, scenario):
    """
    Clear all Krausening environment variables prior to each scenario.
    """
    os.environ["KRAUSENING_BASE"] = ""
    os.environ["KRAUSENING_EXTENSIONS"] = ""
    os.environ["KRAUSENING_OVERRIDE_EXTENSIONS"] = ""
    os.environ["KRAUSENING_PASSWORD"] = ""

    copy_test_resources()


def copy_test_resources():
    source = "tests/resources/apply-encryption/"
    destination = "target/test-classes/resources/apply-encryption/"
    os.makedirs(f"{destination}", exist_ok=True)
    shutil.copy(
        f"{source}test-apply-encryption.properties",
        f"{destination}",
    )
