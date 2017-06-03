#!/bin/sh

SOURCES="			\
    Action.java			\
    ActionAttack.java		\
    ActionFeed.java		\
    ActionMate.java		\
    ActionPuff.java		\
    ActionSelfDestruct.java	\
    ActionShout.java		\
    ActionSpawn.java		\
    Body.java			\
    Brain.java			\
    Creature.java		\
    CreatureAttackee.java	\
    CreatureAttacker.java	\
    CreatureDead.java		\
    CreatureFeeder.java		\
    CreatureFlocker.java	\
    CreatureHomer.java		\
    CreatureHunter.java		\
    CreatureKitchenSink.java	\
    CreatureMisc.java		\
    CreatureRandom.java		\
    CreatureSheep.java		\
    CreatureWolf.java		\
    Gene.java			\
    GeneAction.java		\
    GeneAttack.java		\
    GeneAttackChaff.java	\
    GeneAttackResponse.java	\
    GeneChaseAttack.java	\
    GeneFamilyFollow.java	\
    GeneFeed.java		\
    GeneFindMate.java		\
    GeneFlocking.java		\
    GeneHoming.java		\
    GeneMate.java		\
    GeneMinSpeed.java		\
    GeneMutation.java		\
    GeneNotFamilyFollow.java	\
    GeneRandomPuffAction.java	\
    GeneSizeFollow.java		\
    GeneSelfDestruct.java	\
    GeneShout.java		\
    GeneShoutResponse.java	\
    GeneSpawn.java		\
    KDTree.java			\
    Properties.java		\
    Stimulus.java		\
    StimulusAttacked.java	\
    StimulusFamilies.java	\
    StimulusNeighbours.java	\
    StimulusSelf.java		\
    StimulusShouts.java		\
    StimulusTimeMillis.java	\
    World.java"

# Clean first
\rm -f classes/*.class

# Now make
javac -Xlint:unchecked -classpath /usr/share/java/j3dcore.jar:/usr/share/java/j3dutils.jar:/usr/share/java/vecmath.jar -d classes $SOURCES
