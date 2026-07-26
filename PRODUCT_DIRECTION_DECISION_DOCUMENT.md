# Vision Planner — Product & Architecture Direction Decision Document

> **Status:** Active product source of truth  
> **Date:** 2026-07-26  
> **Updated:** 2026-07-26 (docs cleanup & phase status update)

This document defines product philosophy and product boundaries.  
Technical status lives in `docs/ARCHITECTURE_STATE.md`.  
Future phases live in `docs/ROADMAP.md`.

---

## 1. Product Identity

Vision Planner is **not** a todo app.

It is a **goal-oriented progress system**.

Product value:

> Help users understand their progress, identify obstacles, redesign their path, and move closer to meaningful goals.

Non-negotiable principles:
1. Goal-first, not task-first
2. Low-friction execution
3. Feedback, not judgment
4. Invisible data collection
5. Signal over noise
6. Offline-first

---

## 2. Goal-First Model

- Goal is the primary entity
- Task is the unit of execution
- A task has meaning because of the goal it supports
- Daily execution still happens through tasks
- The product must not become only a goal dashboard

Primary UX model:

```
Goal Card
  └── Today's tasks related to that goal
```

Users should immediately understand why a task exists.

---

## 3. Tasks Without Goals

Tasks without goals are allowed.

They are:
- quick capture / Inbox
- completable
- includable in general statistics
- visually separated from goal-based planning

They must not replace the Goal-first experience.

---

## 4. Event → Insight Model

Core chain:

```
Goal → Task → Event → Snapshot → Mirror → Feedback
```

- Events capture state transitions
- Snapshots project progress/behavior over time
- Mirror detects patterns and produces feedback

Data collection must remain invisible. No forced journals or daily questionnaires.

---

## 5. Mirror Philosophy

Mirror is a calm advisor, not a critic.

Never:
- blame
- criticize performance
- create guilt
- force behavior

Language style:

Bad: "You failed to complete 5 tasks."  
Good: "These tasks have remained incomplete for two weeks. They may need smaller steps."

Mirror V1 principles:
- automatic analysis from existing data
- no separate Mirror screen yet
- feedback inside Goal Dashboard
- no forced reflection

V1 patterns:
1. Boulder
2. Initiator vs Finisher
3. Goal Attention
4. Consistency Decay

---

## 6. Reflection

Reflection is optional and not part of the core loop.

Never design the product around mandatory reflection.

---

## 7. Life Area

Life Area is metadata only:
- optional grouping
- filtering
- labels

Not:
- first-class entity
- required graph node
- fixed mandatory category system

---

## 8. Graph Direction

Graph is a later feature for visual understanding.

Rules:
- computed on demand
- not stored
- currently Goal→Task only
- keep it simple

---

## 9. AI Strategy

AI is a future enhancement layer.

Order:
1. Build valuable non-AI product
2. Collect structured behavioral data
3. Add AI as an intelligence layer

AI must amplify Mirror, not replace it.  
AI is not a V1 dependency.

---

## 10. Product Boundaries

In scope:
- Goal-first planning
- low-friction task execution
- event-based behavioral signal
- Mirror feedback
- offline personal use

Out of scope (current direction):
- todo-app feature race
- forced productivity coaching
- remote/network AI foundation
- social features
- complex knowledge-graph systems
