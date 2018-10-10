package net.consensys.pantheon.consensus.clique;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import net.consensys.pantheon.crypto.SECP256K1.Signature;
import net.consensys.pantheon.ethereum.core.Address;
import net.consensys.pantheon.util.bytes.BytesValue;
import net.consensys.pantheon.util.bytes.BytesValues;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;

/**
 * Represents the data structure stored in the extraData field of the BlockHeader used when
 * operating under an Clique consensus mechanism.
 */
public class CliqueExtraData {

  public static final int EXTRA_VANITY_LENGTH = 32;

  private final BytesValue vanityData;
  private final List<Address> validators;
  private final Optional<Signature> proposerSeal;

  public CliqueExtraData(
      final BytesValue vanityData, final Signature proposerSeal, final List<Address> validators) {

    checkNotNull(vanityData);
    checkNotNull(validators);
    checkArgument(vanityData.size() == EXTRA_VANITY_LENGTH);

    this.vanityData = vanityData;
    this.proposerSeal = Optional.ofNullable(proposerSeal);
    this.validators = validators;
  }

  public static CliqueExtraData decode(final BytesValue input) {
    if (input.size() < EXTRA_VANITY_LENGTH + Signature.BYTES_REQUIRED) {
      throw new IllegalArgumentException(
          "Invalid BytesValue supplied - too short to produce a valid Clique Extra Data object.");
    }

    final int validatorByteCount = input.size() - EXTRA_VANITY_LENGTH - Signature.BYTES_REQUIRED;
    if ((validatorByteCount % Address.SIZE) != 0) {
      throw new IllegalArgumentException(
          "BytesValue is of invalid size - i.e. contains unused bytes.");
    }

    final BytesValue vanityData = input.slice(0, EXTRA_VANITY_LENGTH);
    final List<Address> validators =
        extractValidators(input.slice(EXTRA_VANITY_LENGTH, validatorByteCount));

    final int proposerSealStartIndex = input.size() - Signature.BYTES_REQUIRED;
    final Signature proposerSeal = parseProposerSeal(input.slice(proposerSealStartIndex));

    return new CliqueExtraData(vanityData, proposerSeal, validators);
  }

  private static Signature parseProposerSeal(final BytesValue proposerSealRaw) {
    return proposerSealRaw.isZero() ? null : Signature.decode(proposerSealRaw);
  }

  private static List<Address> extractValidators(final BytesValue validatorsRaw) {
    final List<Address> result = Lists.newArrayList();
    final int countValidators = validatorsRaw.size() / Address.SIZE;
    for (int i = 0; i < countValidators; i++) {
      final int startIndex = i * Address.SIZE;
      result.add(Address.wrap(validatorsRaw.slice(startIndex, Address.SIZE)));
    }
    return result;
  }

  public BytesValue encode() {
    final BytesValue validatorData = BytesValues.concatenate(validators.toArray(new Address[0]));
    return BytesValues.concatenate(
        vanityData,
        validatorData,
        proposerSeal
            .map(Signature::encodedBytes)
            .orElse(BytesValue.wrap(new byte[Signature.BYTES_REQUIRED])));
  }

  public BytesValue getVanityData() {
    return vanityData;
  }

  public Optional<Signature> getProposerSeal() {
    return proposerSeal;
  }

  public List<Address> getValidators() {
    return validators;
  }
}